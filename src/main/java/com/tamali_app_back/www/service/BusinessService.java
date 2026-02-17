package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.BusinessDto;
import com.tamali_app_back.www.entity.Business;
import com.tamali_app_back.www.entity.BusinessSector;
import com.tamali_app_back.www.entity.ReceiptTemplate;
import com.tamali_app_back.www.enums.LegalStatus;
import com.tamali_app_back.www.exception.ResourceNotFoundException;
import com.tamali_app_back.www.repository.BusinessRepository;
import com.tamali_app_back.www.repository.BusinessSectorRepository;
import com.tamali_app_back.www.repository.ReceiptTemplateRepository;
import com.tamali_app_back.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final ReceiptTemplateRepository receiptTemplateRepository;
    private final BusinessSectorRepository businessSectorRepository;
    private final UserRepository userRepository;
    private final EntityMapper mapper;
    private final SupabaseStorageService supabaseStorage;

    @Transactional(readOnly = true)
    public List<BusinessDto> findAll() {
        return businessRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BusinessDto getById(UUID id) {
        return businessRepository.findById(id).map(mapper::toDto).orElse(null);
    }

    @Transactional
    public BusinessDto create(String name, String email, String phone, String address) {
        Business b = Business.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .address(address)
                .active(true)
                .build();
        return mapper.toDto(businessRepository.save(b));
    }

    @Transactional
    public BusinessDto createForUser(String name, String email, UUID userId) {
        Business savedBusiness = businessRepository.save(Business.builder()
                .name(name)
                .email(email)
                .active(true)
                .build());
        
        // Lier l'entreprise à l'utilisateur
        userRepository.findById(userId).ifPresent(user -> {
            user.setBusiness(savedBusiness);
            userRepository.save(user);
        });
        
        return mapper.toDto(savedBusiness);
    }

    @Transactional
    public BusinessDto uploadLogo(UUID id, MultipartFile file) {
        try {
            Business business = businessRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Entreprise", id));
            String logoUrl = supabaseStorage.uploadLogo(id, file);
            business.setLogoUrl(logoUrl);
            return mapper.toDto(businessRepository.save(business));
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload du logo.", e);
        }
    }

    @Transactional
    public BusinessDto update(UUID id, String name, String email, String phone, String address, Boolean active, String logoUrl) {
        Business b = businessRepository.findById(id).orElse(null);
        if (b == null) return null;
        if (name != null) b.setName(name);
        if (email != null) b.setEmail(email);
        if (phone != null) b.setPhone(phone);
        if (address != null) b.setAddress(address);
        if (active != null) b.setActive(active);
        if (logoUrl != null) b.setLogoUrl(logoUrl);
        return mapper.toDto(businessRepository.save(b));
    }

    /**
     * Met à jour les informations de l'entreprise étape par étape.
     */
    @Transactional
    public BusinessDto updateStep(UUID id, Integer step, String name, UUID sectorId, String address,
                                   String phone, String country, String commerceRegisterNumber,
                                   String identificationNumber, LegalStatus legalStatus,
                                   String bankAccountNumber, String websiteUrl, String logoUrl,
                                   UUID receiptTemplateId) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise", id));

        if (step == 1) {
            if (name != null) business.setName(name);
            if (sectorId != null) {
                BusinessSector sector = businessSectorRepository.findById(sectorId)
                        .orElseThrow(() -> new ResourceNotFoundException("Secteur d'activité", sectorId));
                business.setSector(sector);
            }
        } else if (step == 2) {
            if (address != null) business.setAddress(address);
            if (phone != null) business.setPhone(phone);
            if (country != null) business.setCountry(country);
        } else if (step == 3) {
            if (commerceRegisterNumber != null) business.setCommerceRegisterNumber(commerceRegisterNumber);
            if (identificationNumber != null) business.setIdentificationNumber(identificationNumber);
        } else if (step == 4) {
            if (legalStatus != null) business.setLegalStatus(legalStatus);
            if (bankAccountNumber != null) business.setBankAccountNumber(bankAccountNumber);
            if (websiteUrl != null) business.setWebsiteUrl(websiteUrl);
        } else if (step == 5) {
            if (logoUrl != null) business.setLogoUrl(logoUrl);
        } else if (step == 6) {
            if (receiptTemplateId != null) {
                ReceiptTemplate template = receiptTemplateRepository.findById(receiptTemplateId)
                        .orElseThrow(() -> new ResourceNotFoundException("Template de reçu", receiptTemplateId));
                business.setReceiptTemplate(template);
            }
        }

        return mapper.toDto(businessRepository.save(business));
    }

    @Transactional
    public void deleteById(UUID id) {
        businessRepository.findById(id).ifPresent(b -> {
            b.setDeletedAt(LocalDateTime.now());
            businessRepository.save(b);
        });
    }

    @Transactional
    public BusinessDto updateReceiptTemplate(UUID businessId, UUID templateId) {
        Business business = businessRepository.findById(businessId).orElse(null);
        if (business == null) return null;
        ReceiptTemplate template = receiptTemplateRepository.findById(templateId).orElse(null);
        if (template == null) return null;
        business.setReceiptTemplate(template);
        return mapper.toDto(businessRepository.save(business));
    }
}
