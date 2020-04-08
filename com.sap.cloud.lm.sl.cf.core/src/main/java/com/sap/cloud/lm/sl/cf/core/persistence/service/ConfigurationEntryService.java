package com.sap.cloud.lm.sl.cf.core.persistence.service;

import java.util.List;
import java.util.Objects;

import javax.persistence.EntityManagerFactory;

import org.apache.commons.lang3.ObjectUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata;
import com.sap.cloud.lm.sl.cf.core.persistence.dto.ConfigurationEntryDto;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationEntryQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.impl.ConfigurationEntryQueryImpl;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.Version;

public class ConfigurationEntryService extends PersistenceService<ConfigurationEntry, ConfigurationEntryDto, Long> {

    protected ConfigurationEntryMapper entryMapper;

    public ConfigurationEntryService(EntityManagerFactory entityManagerFactory, ConfigurationEntryMapper entryMapper) {
        super(entityManagerFactory);
        this.entryMapper = entryMapper;
    }

    public ConfigurationEntryQuery createQuery() {
        return new ConfigurationEntryQueryImpl(createEntityManager(), entryMapper);
    }

    @Override
    protected ConfigurationEntryDto merge(ConfigurationEntryDto existingEntry, ConfigurationEntryDto newEntry) {
        super.merge(existingEntry, newEntry);
        String providerNid = ObjectUtils.firstNonNull(removeDefault(newEntry.getProviderNid()), existingEntry.getProviderNid());
        String providerId = ObjectUtils.firstNonNull(newEntry.getProviderId(), existingEntry.getProviderId());
        String targetOrg = ObjectUtils.firstNonNull(newEntry.getTargetOrg(), existingEntry.getTargetOrg());
        String targetSpace = ObjectUtils.firstNonNull(newEntry.getTargetSpace(), existingEntry.getTargetSpace());
        String providerVersion = ObjectUtils.firstNonNull(removeDefault(newEntry.getProviderVersion()), existingEntry.getProviderVersion());
        String content = ObjectUtils.firstNonNull(newEntry.getContent(), existingEntry.getContent());
        String visibility = ObjectUtils.firstNonNull(newEntry.getVisibility(), existingEntry.getVisibility());
        String spaceId = ObjectUtils.firstNonNull(newEntry.getSpaceId(), existingEntry.getSpaceId());
        String contentId = ObjectUtils.firstNonNull(newEntry.getContentId(), existingEntry.getContentId());
        return ConfigurationEntryDto.builder()
                                    .id(newEntry.getPrimaryKey())
                                    .providerNid(providerNid)
                                    .providerId(providerId)
                                    .providerVersion(providerVersion)
                                    .targetOrg(targetOrg)
                                    .targetSpace(targetSpace)
                                    .content(content)
                                    .visibility(visibility)
                                    .spaceId(spaceId)
                                    .contentId(contentId)
                                    .build();
    }

    private String removeDefault(String value) {
        return value.equals(PersistenceMetadata.NOT_AVAILABLE) ? null : value;
    }

    @Override
    protected PersistenceObjectMapper<ConfigurationEntry, ConfigurationEntryDto> getPersistenceObjectMapper() {
        return entryMapper;
    }

    @Override
    protected void onEntityConflict(ConfigurationEntryDto entry, Throwable t) {
        throw new ConflictException(t,
                                    Messages.CONFIGURATION_ENTRY_ALREADY_EXISTS,
                                    entry.getProviderNid(),
                                    entry.getProviderId(),
                                    entry.getProviderVersion(),
                                    entry.getTargetOrg(),
                                    entry.getTargetSpace());
    }

    @Override
    protected void onEntityNotFound(Long id) {
        throw new NotFoundException(Messages.CONFIGURATION_ENTRY_NOT_FOUND, id);
    }

    public static class ConfigurationEntryMapper implements PersistenceObjectMapper<ConfigurationEntry, ConfigurationEntryDto> {

        @Override
        public ConfigurationEntry fromDto(ConfigurationEntryDto dto) {
            Long id = dto.getPrimaryKey();
            String providerNid = getOriginal(dto.getProviderNid());
            String providerId = dto.getProviderId();
            Version version = getParsedVersion(getOriginal(dto.getProviderVersion()));
            CloudTarget targetSpace = new CloudTarget(dto.getTargetOrg(), dto.getTargetSpace());
            String content = dto.getContent();
            List<CloudTarget> visibility = getParsedVisibility(dto.getVisibility());
            String spaceId = dto.getSpaceId();
            String contentId = dto.getContentId();
            return new ConfigurationEntry(id, providerNid, providerId, version, targetSpace, content, visibility, spaceId, contentId);
        }

        protected String getOriginal(String source) {
            return Objects.equals(source, PersistenceMetadata.NOT_AVAILABLE) ? null : source;
        }

        protected Version getParsedVersion(String versionString) {
            return versionString == null ? null : Version.parseVersion(versionString);
        }

        protected List<CloudTarget> getParsedVisibility(String visibility) {
            return visibility == null ? null : JsonUtil.convertJsonToList(visibility, new TypeReference<List<CloudTarget>>() {
            });
        }

        @Override
        public ConfigurationEntryDto toDto(ConfigurationEntry entry) {
            long id = entry.getId();
            String providerNid = getNotNull(entry.getProviderNid());
            String providerId = entry.getProviderId();
            String providerVersion = getNotNull(entry.getProviderVersion());
            String targetOrg = entry.getTargetSpace() == null ? null
                : entry.getTargetSpace()
                       .getOrganizationName();
            String targetSpace = entry.getTargetSpace() == null ? null
                : entry.getTargetSpace()
                       .getSpaceName();
            String content = entry.getContent();
            String visibility = entry.getVisibility() == null ? null : JsonUtil.toJson(entry.getVisibility());
            String spaceId = entry.getSpaceId();
            String contentId = entry.getContentId();
            return ConfigurationEntryDto.builder()
                                        .id(id)
                                        .providerNid(providerNid)
                                        .providerId(providerId)
                                        .providerVersion(providerVersion)
                                        .targetOrg(targetOrg)
                                        .targetSpace(targetSpace)
                                        .content(content)
                                        .visibility(visibility)
                                        .spaceId(spaceId)
                                        .contentId(contentId)
                                        .build();
        }

        private String getNotNull(Object source) {
            return source == null ? PersistenceMetadata.NOT_AVAILABLE : source.toString();
        }

    }

}