package com.exercise.contactBook.service;

import com.exercise.contactBook.crypto.CryptoKeyService;
import com.exercise.contactBook.dto.*;
import com.exercise.contactBook.enums.*;
import com.exercise.contactBook.exception.ResourceNotFoundException;
import com.exercise.contactBook.model.*;
import com.exercise.contactBook.repository.*;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContactService {
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final ContactChangeRequestRepository contactChangeRequestRepository;
    private final Validator validator;
    private final CryptoKeyService cryptoKeyService;
    private final ContactPhoneRepository contactPhoneRepository;
    private final ContactEmailRepository contactEmailRepository;
    private final ContactChangeRequestPhoneRepository contactChangeRequestPhoneRepository;
    private final ContactChangeRequestEmailRepository contactChangeRequestEmailRepository;
    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    private final WorkflowService workflowService;
    private final ApprovalActionRepository approvalActionRepository;

    public ContactService(ContactRepository contactRepository , UserRepository userRepository, ContactChangeRequestRepository contactChangeRequestRepository, Validator validator, CryptoKeyService cryptoKeyService, ContactPhoneRepository contactPhoneRepository, ContactEmailRepository contactEmailRepository, ContactChangeRequestPhoneRepository contactChangeRequestPhoneRepository, ContactChangeRequestEmailRepository contactChangeRequestEmailRepository, WorkflowService workflowService, ApprovalActionRepository approvalActionRepository){
        this.contactRepository = contactRepository ;
        this.userRepository = userRepository;
        this.contactChangeRequestRepository = contactChangeRequestRepository;
        this.validator = validator;
        this.cryptoKeyService = cryptoKeyService;
        this.contactPhoneRepository = contactPhoneRepository;
        this.contactEmailRepository = contactEmailRepository;
        this.contactChangeRequestPhoneRepository = contactChangeRequestPhoneRepository;
        this.contactChangeRequestEmailRepository = contactChangeRequestEmailRepository;
        this.workflowService = workflowService;
        this.approvalActionRepository = approvalActionRepository;
    }


    public List<ContactResponseDTO> getSearchedAndSortedContacts(
            String search,Sort sort, ContactType type) {

        User currentUser = getCurrentUser();
        if(currentUser.getRole() == Role.USER) type = ContactType.PERSONAL;

        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;


        List<Contact> contacts = contactRepository.findContactsFiltered(
                currentUser, currentUser.getOrganization(), type, searchParam, sort
        );

        return contacts.stream()
                .map(this::toContactResponseDTO)
                .toList();
    }

    public ContactResponseDTO getContactById(Long id){
        User currentUser = getCurrentUser();
        Contact contact = contactRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("resource not found with id :- " + id));
        if(!isVisible(contact, currentUser)){
            throw new ResourceNotFoundException("resource not found with id : " + id);
        }
        return toContactResponseDTO(contact);
    }

    public ContactWriteResultDTO addContact(ContactRequestDTO dto){
        User currentUser = getCurrentUser();

        return addSingleContact(dto,currentUser);
    }

    public ContactWriteResultDTO addSingleContact(ContactRequestDTO dto , User currentUser){

      if(currentUser.getRole() == Role.USER && dto.getType() == ContactType.COMMON){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Personal accounts can only have personal contacts");
        }
        validateHasContact(dto);
        normalizeDtoPhones(dto, currentUser);
        checkForDuplicateInRequest(dto);
        checkForDuplicates(dto,currentUser,null);

        ApprovalWorkflow workflow = null ;
        if(dto.getType() == ContactType.COMMON) {
            workflow = workflowService.findFor(currentUser, ContactActionType.CREATE);
            if (workflow == null) {
                requireDirectCommonAccess(currentUser, "You dont have permission to add a common contact");
            }
        }
        if(workflow != null){
            for(PhoneDTO p : dto.getPhones()) {
                if (contactChangeRequestPhoneRepository
                        .existsByPhoneAndChangeRequest_OrganizationAndChangeRequestStatus(
                                p.getPhone(), currentUser.getOrganization(), RequestStatus.PENDING
                        )) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A request for this phone number : " + p.getPhone() + " is already pending for approval");
                }
            }

            for(EmailDTO e : dto.getEmails()) {
                if (contactChangeRequestEmailRepository
                        .existsByEmailAndChangeRequest_OrganizationAndChangeRequestStatus(
                                e.getEmail(), currentUser.getOrganization(), RequestStatus.PENDING
                        )) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A request for this email : " + e.getEmail() + " is already pending for approval");
                }
            }
            ContactChangeRequest request = new ContactChangeRequest();
            request.setActionType(ContactActionType.CREATE);
            request.setRequestedName(dto.getName());
            request.setRequestedNotes(dto.getNotes());
            request.setOrganization(currentUser.getOrganization());
            request.setRequestedBy(currentUser);
            request.setWorkflow(workflow);
            request.setCurrentStep(1);
            applyRequestedPhonesAndEmails(request,dto);

            ContactChangeRequest saved = contactChangeRequestRepository.save(request);
            return new ContactWriteResultDTO(true,null,toResponseDTO(saved));

        }
        Contact contact = new Contact();
      applyPhonesAndEmails(contact,dto);
        contact.setName(dto.getName());
        contact.setNotes(dto.getNotes());
        contact.setOrganization(currentUser.getOrganization());
        contact.setType(dto.getType());
        //changing the ownership for common contact to organization
        contact.setUser(dto.getType() == ContactType.COMMON
        ? currentUser.getOrganization().getAdmin()
                : currentUser);
        Contact savedContact = contactRepository.save(contact);
        return new ContactWriteResultDTO(false, toContactResponseDTO(savedContact) , null);
    }

    public ContactWriteResultDTO updateContact(Long id, ContactRequestDTO dto) {
        User currentUser = getCurrentUser();

        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource not found with id :- " + id));

        if (!isVisible(contact, currentUser)) {
            throw new ResourceNotFoundException("resource not found with id : " + id);
        }
        validateHasContact(dto);
        normalizeDtoPhones(dto,currentUser);
        checkForDuplicateInRequest(dto);
        ApprovalWorkflow workflow = null;
        if (contact.getType() == ContactType.COMMON ) {
            workflow = workflowService.findFor(currentUser, ContactActionType.UPDATE);

            if (workflow == null) {
                requireDirectCommonAccess(currentUser, "you dont have permission to edit a common common contact");
            }
        }
        if(workflow != null){
            checkForDuplicates(dto,currentUser,contact.getId());

            if(contactChangeRequestRepository.existsByTargetContactAndStatus(
                    contact,RequestStatus.PENDING
            )){
                throw new ResponseStatusException(HttpStatus.CONFLICT , "A change request for this contact is already pending for approval");
            }
            ContactChangeRequest request = new ContactChangeRequest();
            request.setActionType(ContactActionType.UPDATE);
            request.setTargetContact(contact);
            request.setRequestedName(dto.getName() != null ? dto.getName() : contact.getName());
            request.setRequestedNotes(dto.getNotes() != null ? dto.getNotes() : contact.getNotes());
            request.setOrganization(currentUser.getOrganization());
            request.setRequestedBy(currentUser);
            request.setWorkflow(workflow);
            request.setCurrentStep(1);
            applyRequestedPhonesAndEmails(request,dto);

            ContactChangeRequest saved = contactChangeRequestRepository.save(request);
            return new ContactWriteResultDTO(true,null,toResponseDTO(saved));
        }

        checkForDuplicates(dto, currentUser, contact.getId());

        if (dto.getName() != null) {
            contact.setName(dto.getName());
        }
        if (dto.getNotes() != null) {
            contact.setNotes(dto.getNotes());
        }
        applyPhonesAndEmails(contact, dto);

        Contact savedContact = contactRepository.save(contact);
        return new ContactWriteResultDTO(false , toContactResponseDTO((savedContact)), null);
    }

    public void deleteContact(Long id){
        User currentUser = getCurrentUser();
        Contact contact = contactRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("resource not found with id :- " + id));
        if(!isVisible(contact, currentUser)){
            throw new ResourceNotFoundException("resource not found with id : " + id);
        }
        if(contact.getType() == ContactType.COMMON){
           requireDirectCommonAccess(currentUser, "you dont have permission to delete a common contactget");
        }

        //to auto reject the request for this contact before deleting it
        //also setting target contact to null to avoid the foriegn key violation
        List<ContactChangeRequest> linkedRequests = contactChangeRequestRepository.findByTargetContact(contact);
        for (ContactChangeRequest req : linkedRequests){
            if(req.getStatus() == RequestStatus.PENDING) {
                req.setStatus(RequestStatus.REJECTED);
                req.setReviewNote("the contact was deleted before this request could be reviewed");
            }
            req.setTargetContact(null);
            contactChangeRequestRepository.save(req);
        }
        contactRepository.deleteById(id);
    }


    private BulkImportResultDTO processRows(List<RowItem> items, User currentUser){
        int created = 0 , pendingApproval = 0, skipped = 0;
        List<BulkImportRowErrorDTO> errors = new ArrayList<>();
        Set<String> seenPhones = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();
        for (RowItem item : items) {
            ContactRequestDTO dto = item.dto;
            int rowNumber = item.rowNumber();
            String duplicateValue = null;
            for(PhoneDTO p : dto.getPhones()){
            if(seenPhones.contains(p.getPhone())){
                duplicateValue = p.getPhone();
                break;
            }
            }
            if(duplicateValue == null){
                for ( EmailDTO e : dto.getEmails()){
                    if(seenEmails.contains(e.getEmail().toLowerCase())){
                        duplicateValue = e.getEmail().toLowerCase();
                        break;
                    }
                }
            }
            if(duplicateValue != null){
                errors.add(new BulkImportRowErrorDTO(rowNumber, "Duplicate phone/email within this file " + duplicateValue));
                skipped++;
                continue;
            }
            for (PhoneDTO p : dto.getPhones()){
                seenPhones.add(p.getPhone());
            }
            for (EmailDTO e : dto.getEmails()){
                seenEmails.add(e.getEmail());
            }
            try{
                ContactWriteResultDTO result = addSingleContact(dto,currentUser);
                if(result.isPendingApproval()){
                    pendingApproval++;
                }else{
                    created++;
                }
            }catch (ResponseStatusException ex){
                errors.add(new BulkImportRowErrorDTO(rowNumber , ex.getReason()));
                skipped++;
            }
        }
        return new BulkImportResultDTO(items.size(),created,pendingApproval,skipped,errors);
    }

    public  BulkImportResultDTO importEncryptedCsv(EncryptedCSVRequestDTO dto){
        byte[] aesKeyBytes;
        byte[] plainText;
        try {
            aesKeyBytes = cryptoKeyService.decryptAESKey( dto.getEncryptedKey(), dto.getKeyId());
            plainText = cryptoKeyService.decrypt(aesKeyBytes, dto.getIv(), dto.getCipherText());
        }catch (IllegalArgumentException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        if(plainText.length >  5 * 1024 * 1024){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "File too large (max 5MB)");
        }
        return parseAndImportCsv(new ByteArrayInputStream(plainText));
    }


    public List<ContactChangeRequestDTO> getPendingRequests(){
        User currentUser = getCurrentUser();
        return contactChangeRequestRepository.findByOrganizationAndStatus(currentUser.getOrganization(), RequestStatus.PENDING)
                .stream()
                .filter( r -> canActOn(r, currentUser))
                .map(this::toResponseDTO).toList();
    }

    public List<ContactChangeRequestDTO> getMyRequest(){
        User currentUser = getCurrentUser();
        return contactChangeRequestRepository.findByRequestedBy(currentUser)
                .stream()
                .map(this::toResponseDTO).toList();
    }

    public void cancelRequest(Long requestId){
        User currentUser = getCurrentUser();
        ContactChangeRequest request = contactChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: "+ requestId));

        if(!request.getRequestedBy().getId().equals(currentUser.getId())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You can only cancel your own requests");
        }
        contactChangeRequestRepository.delete(request);
    }

    @Transactional
    public ContactResponseDTO approveRequest(Long requestId){
        User approver = getCurrentUser();
        ContactChangeRequest request = getOwnOrgPendindRequests(requestId, approver);

        if(request.getRequestedBy().getId().equals(approver.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You cannot approve your own request");
        }

        if(!canActOn(request, approver)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This request is not  waiting for your approval");
        }
        int step = stepNumber(request);

        int totalSteps = request.getWorkflow() == null ? 1 :request.getWorkflow().getSteps().size();
        boolean finishesNow = isOrgAdmin(approver) || isAnyOne(request) || step >= totalSteps;
        if(!finishesNow){
            request.setCurrentStep(step + 1 );
            contactChangeRequestRepository.save(request);
            return null;
        }
       return applyApprovedRequest(request, approver);
    }

    public ContactResponseDTO applyApprovedRequest(ContactChangeRequest request, User approver){
        Contact contact;
        if(request.getActionType() == ContactActionType.CREATE){
            for(ContactChangeRequestPhone p : request.getRequestedPhones()) {
                if (contactPhoneRepository.existsByPhoneAndContact_OrganizationAndContact_Type(
                        p.getPhone(), approver.getOrganization(), ContactType.COMMON
                )) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A common contact with this phone number : " + p.getPhone() + "already exists in this organization - cannot approve");
                }
            }

            for(ContactChangeRequestEmail e : request.getRequestedEmails()) {
                if (contactEmailRepository.existsByEmailAndContact_OrganizationAndContact_Type(
                        e.getEmail(), approver.getOrganization(), ContactType.COMMON
                )) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A common contact with this email : " + e.getEmail() + "already exists in this organization - cannot approve");
                }
            }
            contact = new Contact();
            contact.setType(ContactType.COMMON);
            contact.setUser(request.getOrganization().getAdmin());
            contact.setOrganization(request.getOrganization());
        }else{
            contact = request.getTargetContact();
            if(contact == null){
                request.setStatus(RequestStatus.REJECTED);
                request.setReviewNote("the target contact no longer exists");
                contactChangeRequestRepository.save(request);
                throw new ResponseStatusException(HttpStatus.CONFLICT , "This contact was deleted - contact auto rejected");
            }
            for(ContactChangeRequestPhone p : request.getRequestedPhones()) {
                if (contactPhoneRepository.existsByPhoneAndContact_OrganizationAndContact_TypeAndContact_IdNot(
                        p.getPhone(), approver.getOrganization(), ContactType.COMMON, contact.getId()
                )) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A common contact with this phone number : "+ p.getPhone()+ " already exists in this organization - cannot approve");
                }
            }

            for(ContactChangeRequestEmail e : request.getRequestedEmails()) {
                if (contactEmailRepository.existsByEmailAndContact_OrganizationAndContact_TypeAndContact_IdNot(
                        e.getEmail() , approver.getOrganization(), ContactType.COMMON, contact.getId()
                )) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A common contact with this email : "+e.getEmail()+ " already exists in this organization - cannot approve");
                }
            }
        }
        contact.setName(request.getRequestedName());
        contact.setNotes(request.getRequestedNotes());
        applyPhoneAndEmailsFromRequest(contact,request);
        Contact savedContact = contactRepository.save(contact);


        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedBy(approver);
        contactChangeRequestRepository.save(request);
        return toContactResponseDTO(savedContact);
    }

    public void rejectRequest(Long requestId, ReviewRequestDTO dto){
        String note = dto != null ? dto.getNote() : null ;
        User approver = getCurrentUser();
        ContactChangeRequest request = getOwnOrgPendindRequests(requestId, approver);
        if(!canActOn(request , approver)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This request is not waiting for your approval");
        }
        recordAction(request, stepNumber(request), approver, ApprovalDecision.REJECTED, note);
        request.setStatus(RequestStatus.REJECTED);
        request.setReviewedBy(approver);
        request.setReviewNote(note);
        contactChangeRequestRepository.save(request);
    }

    public ContactChangeRequest getOwnOrgPendindRequests(Long requestId, User approver){
        ContactChangeRequest request = contactChangeRequestRepository.findById(requestId)
                .orElseThrow(()-> new ResourceNotFoundException("Request not found with id :  " + requestId));

        if(!request.getOrganization().getId().equals(approver.getOrganization().getId())){
            throw new ResourceNotFoundException("Request not found with id :  " + requestId);
        }

        if(request.getStatus() != RequestStatus.PENDING){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request has already been reviewed");
        }
        return request;
    }

    //helper methodss

    private void normalizeDtoPhones (ContactRequestDTO dto, User currentUser){
        for(PhoneDTO p : dto.getPhones()){
            p.setPhone(normalizeToE164(p.getPhone()));
        }
    }

    private String normalizeToE164(String phone){
        String value = phone == null ? "" : phone.trim();
        if(!value.startsWith("+")){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "Phone number must Include the country code ");
        }
        try{
            var parsed = phoneNumberUtil.parse(value , null);
            if(!phoneNumberUtil.isValidNumber(parsed)){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid phone number: " + phone);
            }
            return phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "Invalid phone number : " + phone);
        }
    }

    private BulkImportResultDTO parseAndImportCsv(InputStream inputStream) {

        User currentUser = getCurrentUser();
        List<RowItem> items = new ArrayList<>();
        List<BulkImportRowErrorDTO> parseErrors = new ArrayList<>();

        try (var reader = new java.io.InputStreamReader(inputStream);
             var parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .setIgnoreHeaderCase(true)
                     .setIgnoreSurroundingSpaces(true)
                     .get()
                     .parse(reader)) {

            int rowNum = 1;
            for (var record : parser) {
                rowNum++;
                if (rowNum - 1 > 5000) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV has too many rows(MAX 5000)");
                }
                try {
                    if (record.stream().allMatch(String::isBlank)) {
                        continue;
                    }
                    ContactRequestDTO dto = new ContactRequestDTO();
                    dto.setName(record.isMapped("name") && record.isSet("name") ? record.get("name") : null);
                    dto.setNotes(record.isMapped("notes") && record.isSet("notes") ? record.get("notes") : null);
                    dto.setPhones(parsePhoneCell(record.isMapped("phones") && record.isSet("phones") ? record.get("phones") : null));
                    dto.setEmails(parseEmailCell(record.isMapped("emails") && record.isSet("emails") ? record.get("emails") : null));

                    String type = record.isMapped("type") && record.isSet("type") ? record.get("type") : null;
                    try {
                        dto.setType(ContactType.valueOf(type.trim().toUpperCase()));
                    } catch (Exception e) {
                        parseErrors.add(new BulkImportRowErrorDTO(rowNum, "Invalid or missing type (must be PERSONAL or COMMON)"));
                        continue;
                    }

                    var violations = validator.validate(dto);
                    if (!violations.isEmpty()) {
                        String message = violations.stream()
                                .map(v -> v.getMessage())
                                .collect(java.util.stream.Collectors.joining(", "));
                        parseErrors.add(new BulkImportRowErrorDTO(rowNum, message));
                        continue;
                    }
                    items.add(new RowItem(rowNum, dto));
                }catch (ResponseStatusException ex){
                    throw ex;
                }catch (Exception ex){
                    parseErrors.add(new BulkImportRowErrorDTO(rowNum , "could not read this row " + ex.getMessage()));
                }
            }
        }
        catch (java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read CSV file: " + e.getMessage());
        }

        BulkImportResultDTO result = processRows(items, currentUser);
        result.getErrors().addAll(0, parseErrors);
        result.setTotalRows(result.getTotalRows() + parseErrors.size());
        result.setSkipped(result.getSkipped() + parseErrors.size());
        return result;
    }

    private List<PhoneDTO> parsePhoneCell(String cell){
        List<PhoneDTO> list = new ArrayList<>();
        if(cell == null || cell.isBlank()){
            return list;
        }
        for(String entry : cell.split(";")){
            entry = entry.trim();
            if(entry.isEmpty()) continue;
            boolean isprimary = entry.startsWith("*");
            if(isprimary) {
                while(entry.startsWith("*")) {
                    entry = entry.substring(1).trim();
                }
                }

            PhoneDTO dto = new PhoneDTO();
            if(entry.contains(":")){
                String []parts = entry.split(":",2);
                try{
                    PhoneLable lable = PhoneLable.valueOf(parts[0].trim().toUpperCase());
                    dto.setLable(lable);

                }catch(Exception ex){
                    dto.setLable(PhoneLable.MOBILE);
                }
                dto.setPhone(parts[1].trim());
            }else{
                dto.setLable(PhoneLable.MOBILE);
                dto.setPhone(entry);
            }
            dto.setPrimary(isprimary);
            list.add(dto);
        }
        return list;
    }

    private List<EmailDTO> parseEmailCell(String cell){
        List<EmailDTO> list = new ArrayList<>();
        if(cell == null || cell.isBlank()){
            return list;
        }
        for(String entry : cell.split(";")){
            entry = entry.trim();
            if(entry.isEmpty()) continue;
            boolean isprimary = entry.startsWith("*");
            if(isprimary) {
                while(entry.startsWith("*")) {
                    entry = entry.substring(1).trim();
                }
            }

            EmailDTO dto = new EmailDTO();
            if(entry.contains(":")){
                String []parts = entry.split(":",2);
                try{
                    EmailLable lable = EmailLable.valueOf(parts[0].trim().toUpperCase());
                    dto.setLable(lable);

                }catch(Exception ex){
                    dto.setLable(EmailLable.OTHER);
                }
                dto.setEmail(parts[1].trim());
            }
            else{
                dto.setLable(EmailLable.OTHER);
                dto.setEmail(entry);
            }
            dto.setPrimary(isprimary);
            list.add(dto);
        }
        return list;
    }

    private record RowItem(int rowNumber, ContactRequestDTO dto){}

    private void validateHasContact(ContactRequestDTO dto){
        Boolean hasPhone = dto.getPhones()!= null && !dto.getPhones().isEmpty();
        Boolean hasEmail = dto.getEmails()!= null && !dto.getEmails().isEmpty();
        if(!hasEmail && !hasPhone){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "Atleast one email or phone is required");
        }
    }

    private void checkForDuplicateInRequest(ContactRequestDTO dto){
        Set<String> checkedPhones = new HashSet<>();
        for(PhoneDTO p : dto.getPhones()){
            if(!checkedPhones.add(p.getPhone())){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "Duplicate phone number within this contact : " + p.getPhone());
            }

        }
        Set<String> checkedEmails = new HashSet<>();
        for(EmailDTO email : dto.getEmails()){
            String lowercaseEmail = email.getEmail().toLowerCase();
            if(!checkedEmails.add(lowercaseEmail)){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "Duplicate email within this contact : " + email.getEmail());
            }

        }

    }

    private void checkForDuplicates(ContactRequestDTO dto , User currentUser, Long excludeContactId){
        boolean isCommon = dto.getType() == ContactType.COMMON;

        for(PhoneDTO phone : dto.getPhones()){
            boolean exists = isCommon ?
                    (excludeContactId == null ?
                           contactPhoneRepository.existsByPhoneAndContact_OrganizationAndContact_Type
                                   (phone.getPhone()
                                   , currentUser.getOrganization(),ContactType.COMMON)
                            : contactPhoneRepository.existsByPhoneAndContact_OrganizationAndContact_TypeAndContact_IdNot
                            (phone.getPhone(),
                                    currentUser.getOrganization(),ContactType.COMMON, excludeContactId)
                    )
                    :  (excludeContactId == null ?
                    contactPhoneRepository.existsByPhoneAndContact_UserAndContact_Type(phone.getPhone()
                                    , currentUser,ContactType.PERSONAL)
                    : contactPhoneRepository.existsByPhoneAndContact_UserAndContact_TypeAndContact_IdNot
                        (phone.getPhone(),
                            currentUser,ContactType.PERSONAL, excludeContactId)
            );
            if(exists){
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        (isCommon ? "A common contact exists in your org with phone"
                                : "You already have personal contact with phone : ") + phone.getPhone() );
            }
        }
        for(EmailDTO e : dto.getEmails()){
            String email = e.getEmail().toLowerCase();
            boolean exists = isCommon ?
                    (excludeContactId == null ?
                            contactEmailRepository.existsByEmailAndContact_OrganizationAndContact_Type
                                    (email
                                            , currentUser.getOrganization(),ContactType.COMMON)
                            : contactEmailRepository.existsByEmailAndContact_OrganizationAndContact_TypeAndContact_IdNot
                            (email,
                                    currentUser.getOrganization(),ContactType.COMMON, excludeContactId)
                    )
                    :  (excludeContactId == null ?
                    contactEmailRepository.existsByEmailAndContact_UserAndContact_Type(email
                            , currentUser,ContactType.PERSONAL)
                    : contactEmailRepository.existsByEmailAndContact_UserAndContact_TypeAndContact_IdNot
                    (email,
                            currentUser,ContactType.PERSONAL, excludeContactId)
            );
            if(exists){
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        (isCommon ? "A common contact exists in your org with email"
                                : "You already have personal contact with email : ") + email);
            }
        }

    }

    private void applyPhonesAndEmails(Contact contact, ContactRequestDTO dto){
        contact.getPhones().clear();
        contact.getEmails().clear();

        boolean primaryPhoneSet = false;

        for(PhoneDTO p : dto.getPhones()){
            ContactPhone phone = new ContactPhone();
            phone.setContact(contact);
            phone.setPhone(p.getPhone());
            phone.setLable(p.getLable());
            boolean isPrimary = p.isPrimary() && !primaryPhoneSet;
            phone.setPrimary(isPrimary);
            if(isPrimary) primaryPhoneSet = true;
            contact.getPhones().add(phone);
        }
        if(!primaryPhoneSet && !contact.getPhones().isEmpty()){
            contact.getPhones().get(0).setPrimary(true);
        }

        boolean primaryEmailSet = false;

        for(EmailDTO e : dto.getEmails()){
            ContactEmail email = new ContactEmail();
            email.setContact(contact);
            email.setEmail(e.getEmail().toLowerCase());
            email.setLable(e.getLable());
            boolean isPrimary = e.isPrimary() && !primaryEmailSet;
            email.setPrimary(isPrimary);
            if(isPrimary) primaryEmailSet = true;
            contact.getEmails().add(email);
        }
        if(!primaryEmailSet && !contact.getEmails().isEmpty()){
            contact.getEmails().get(0).setPrimary(true);
        }
    }

    private void applyRequestedPhonesAndEmails(ContactChangeRequest request , ContactRequestDTO dto){
        for (PhoneDTO p : dto.getPhones()){
            ContactChangeRequestPhone phone = new ContactChangeRequestPhone();
            phone.setChangeRequest(request);
            phone.setPhone(p.getPhone());
            phone.setLable(p.getLable());
            phone.setPrimary(p.isPrimary());
            request.getRequestedPhones().add(phone);
        }

        for (EmailDTO e : dto.getEmails()){
            ContactChangeRequestEmail email = new ContactChangeRequestEmail();
            email.setChangeRequest(request);
            email.setEmail(e.getEmail().toLowerCase());
            email.setLable(e.getLable());
            email.setPrimary(e.isPrimary());
            request.getRequestedEmails().add(email);
        }
    }

    private void applyPhoneAndEmailsFromRequest(Contact contact, ContactChangeRequest request){
        contact.getEmails().clear();
        contact.getPhones().clear();

        boolean primaryPhoneSet = false;
        for(ContactChangeRequestPhone p : request.getRequestedPhones()){
            ContactPhone phone =  new ContactPhone();
            phone.setContact(contact);
            phone.setPhone(p.getPhone());
            phone.setLable(p.getLable());
            boolean isPrimary = p.isPrimary() && !primaryPhoneSet;
            phone.setPrimary(isPrimary);
            if(isPrimary) primaryPhoneSet = true;
            contact.getPhones().add(phone);
        }
        if(!primaryPhoneSet && !contact.getPhones().isEmpty()){
            contact.getPhones().get(0).setPrimary(true);
        }

        boolean primaryEmailSet = false;
        for(ContactChangeRequestEmail e : request.getRequestedEmails()){
            ContactEmail email =  new ContactEmail();
            email.setContact(contact);
            email.setEmail(e.getEmail().toLowerCase());
            email.setLable(e.getLable());
            boolean isPrimary = e.isPrimary() && !primaryEmailSet;
            email.setPrimary(isPrimary);
            if(isPrimary) primaryEmailSet = true;
            contact.getEmails().add(email);
        }
        if(!primaryEmailSet && !contact.getEmails().isEmpty()){
            contact.getEmails().get(0).setPrimary(true);
        }
    }

    private List<PhoneDTO> toPhoneDTOs(Contact contact){
        return contact.getPhones().stream()
                .map(p ->{
                    PhoneDTO dto = new PhoneDTO();
                    dto.setPhone(p.getPhone());
                    dto.setLable(p.getLable());
                    dto.setPrimary(p.isPrimary());
                    return dto;
                }).toList();
    }

    private List<EmailDTO> toEmailDTOs(Contact contact){
        return contact.getEmails().stream()
                .map(e ->{
                    EmailDTO dto = new EmailDTO();
                    dto.setEmail(e.getEmail());
                    dto.setLable(e.getLable());
                    dto.setPrimary(e.isPrimary());
                    return dto;
                }).toList();
    }

    private ContactChangeRequestDTO toResponseDTO(ContactChangeRequest request){
        ContactChangeRequestDTO dto = new ContactChangeRequestDTO();
        dto.setId(request.getId());
        dto.setActionType(request.getActionType());
        dto.setTargetContactId(request.getTargetContact() != null ? request.getTargetContact().getId() : null);
        dto.setRequestedName(request.getRequestedName());
        dto.setRequestedNotes(request.getRequestedNotes());

        dto.setRequestedPhones(request.getRequestedPhones().stream()
                .map(p ->{
                    PhoneDTO phoneDTO = new PhoneDTO();
                    phoneDTO.setLable(p.getLable());
                    phoneDTO.setPhone(p.getPhone());
                    phoneDTO.setPrimary(p.isPrimary());
                    return phoneDTO;
                }).toList());

        dto.setRequestedEmails(request.getRequestedEmails().stream()
                .map(e ->{
                    EmailDTO emailDTO = new EmailDTO();
                    emailDTO.setEmail(e.getEmail());
                    emailDTO.setLable(e.getLable());
                    emailDTO.setPrimary(e.isPrimary());
                    return emailDTO;
                }).toList());
        dto.setStatus(request.getStatus());
        dto.setRequestedByName(request.getRequestedBy().getName());
        dto.setReviewNote(request.getReviewNote());

        ApprovalWorkflow workflow = request.getWorkflow();
        if(workflow != null){
            boolean any = isAnyOne(request);
            int currentStep = stepNumber(request);
            dto.setWorkflowName(workflow.getName());
            dto.setMode(any? ApprovalMode.ANY_ONE : ApprovalMode.SEQUENTIAL);
            dto.setCurrentStep(currentStep);
            dto.setTotalSteps(workflow.getSteps().size());

            List<StepProgressDTO> steps = new ArrayList<>();
            for(ApprovalStep s : workflow.getSteps()){
                String state;
                if(request.getStatus() == RequestStatus.APPROVED) state = "DONE";
                else if (any) state = request.getStatus() == RequestStatus.REJECTED ? "UPCOMING" : "CURRENT";
                else if (s.getStepOrder() < currentStep) {
                    state = "DONE";
                } else if (s.getStepOrder() == currentStep) {
                    state = request.getStatus() == RequestStatus.REJECTED ? "REJECTED" : "CURRENT";
                }
                else state = "UPCOMING";
                steps.add(new StepProgressDTO(s.getStepOrder(), s.getApproverRole().getName(), state));
            }
            dto.setSteps(steps);
            if (request.getStatus() == RequestStatus.PENDING){
                dto.setWaitingFor(any
                ? workflow.getSteps().stream().map(s -> s.getApproverRole().getName()).collect(Collectors.joining(" or "))
                        : workflow.getSteps().stream().filter(s -> s.getStepOrder() == currentStep)
                        .map(s-> s.getApproverRole().getName()).findFirst().orElse(null));
            }
        }

        return dto;
    }

//    private boolean needsApproval(User user){
//        return (user.getOrgRole() == null ||
//                !user.getOrgRole().getPermissions().contains(Permission.CONTACT_COMMON_DIRECT));
//    }

    private boolean isOrgAdmin(User user){
        return user.getOrgRole() != null &&  "ADMIN".equalsIgnoreCase(user.getOrgRole().getName());
    }
    private void requireDirectCommonAccess(User user, String msg){
        if(user.getOrgRole() == null || !user.getOrgRole().getPermissions().contains(Permission.CONTACT_COMMON_DIRECT)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, msg);
        }
    }
    private int stepNumber(ContactChangeRequest r) {
        return r.getCurrentStep() == null ? 1 : r.getCurrentStep();
    }


    private ApprovalStep currentStep(ContactChangeRequest r) {
        if (r.getWorkflow() == null) return null;
        return r.getWorkflow().getSteps().stream()
                .filter(s -> s.getStepOrder() == stepNumber(r))
                .findFirst().orElse(null);
    }

    private boolean isAnyOne(ContactChangeRequest r){
        return  r.getWorkflow() != null && r.getWorkflow().getMode() == ApprovalMode.ANY_ONE;
    }

    // Can this user approve or reject this request right now?
    private boolean canActOn(ContactChangeRequest r, User me) {
        if (r.getRequestedBy().getId().equals(me.getId())) return false;   // never your own
        if (isOrgAdmin(me)) return true;                                    // safety valve
        if(me.getOrgRole() == null || r.getWorkflow() == null) return false;
        Long myRoleId = me.getOrgRole().getId();
        if(isAnyOne(r)){
            return r.getWorkflow().getSteps().stream()
                    .anyMatch(s -> s.getApproverRole().getId().equals(myRoleId));
        }
        ApprovalStep step = currentStep(r);
        return step != null && step.getApproverRole().getId().equals(me.getOrgRole().getId());
    }


    private void recordAction(ContactChangeRequest request, int step, User actor,
                              ApprovalDecision decision, String note) {
        ApprovalAction a = new ApprovalAction();
        a.setRequest(request);
        a.setStepOrder(step);
        a.setActorId(actor.getId());
        a.setActorName(actor.getName());
        a.setDecision(decision);
        a.setNote(note);
        approvalActionRepository.save(a);
    }
    private boolean isVisible(Contact contact , User currentUser){

        if(contact.getType() == ContactType.PERSONAL){
            return contact.getUser().getId().equals(currentUser.getId());
        }
        if(currentUser.getOrganization() == null || contact.getOrganization() == null) return false;
        return contact.getOrganization().getId().equals(currentUser.getOrganization().getId());
    }

    private ContactResponseDTO toContactResponseDTO(Contact contact){
        ContactResponseDTO contactResponseDTO =  new ContactResponseDTO();
        contactResponseDTO.setId(contact.getId());
        contactResponseDTO.setEmails(toEmailDTOs(contact));
        contactResponseDTO.setName(contact.getName());
        contactResponseDTO.setNotes(contact.getNotes());
        contactResponseDTO.setPhones(toPhoneDTOs(contact));
        contactResponseDTO.setUserId(contact.getUser().getId());
        contactResponseDTO.setType(contact.getType());
        return contactResponseDTO;
    }
    private User getCurrentUser(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found"));
    }

}
