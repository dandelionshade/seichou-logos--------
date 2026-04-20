package com.seichou.logos.service;

import com.seichou.logos.entity.User;
import com.seichou.logos.entity.UserPreferences;
import com.seichou.logos.repository.UserPreferencesRepository;
import com.seichou.logos.repository.UserRepository;
import com.seichou.logos.security.ApiKeyCryptoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserPreferenceService {

    private final UserPreferencesRepository repository;
    private final UserRepository userRepository;
    private final ApiKeyCryptoService apiKeyCryptoService;

    public UserPreferenceService(
            UserPreferencesRepository repository,
            UserRepository userRepository,
            ApiKeyCryptoService apiKeyCryptoService
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.apiKeyCryptoService = apiKeyCryptoService;
    }

    public UserPreferences getPreferences(UUID userId) {
        UserPreferences preferences = repository.findById(userId)
                .orElseGet(() -> {
                User user = userRepository.findById(userId).orElseThrow();
                UserPreferences created = new UserPreferences();
                created.setUser(user);
                created.setUserId(userId);
                created.setTheme("dark");
                created.setLanguage("en");
                created.setAiPersonality("empathetic");
                created.setNotificationsEnabled(true);
                created.setDataPrivacy("standard");
                return repository.save(created);
                });

        return migrateApiKeyToCiphertextIfNeeded(preferences);
    }

    @Transactional
    public UserPreferences updatePreferences(UUID userId, UserPreferences updates) {
        UserPreferences prefs = getPreferences(userId);
        if (updates.getTheme() != null) prefs.setTheme(updates.getTheme());
        if (updates.getLanguage() != null) prefs.setLanguage(updates.getLanguage());
        if (updates.getAiPersonality() != null) prefs.setAiPersonality(updates.getAiPersonality());
        if (updates.getNotificationsEnabled() != null) prefs.setNotificationsEnabled(updates.getNotificationsEnabled());
        if (updates.getDataPrivacy() != null) prefs.setDataPrivacy(updates.getDataPrivacy());
        if (updates.getDeepseekApiKey() != null) {
            prefs.setDeepseekApiKey(apiKeyCryptoService.encrypt(updates.getDeepseekApiKey()));
        }
        return repository.save(prefs);
    }

    public String resolveDeepseekApiKey(UUID userId) {
        UserPreferences prefs = getPreferences(userId);
        return apiKeyCryptoService.decryptIfNeeded(prefs.getDeepseekApiKey());
    }

    private UserPreferences migrateApiKeyToCiphertextIfNeeded(UserPreferences prefs) {
        String stored = prefs.getDeepseekApiKey();
        if (stored == null || stored.isBlank() || apiKeyCryptoService.isEncrypted(stored)) {
            return prefs;
        }

        prefs.setDeepseekApiKey(apiKeyCryptoService.encrypt(stored));
        return repository.save(prefs);
    }
}
