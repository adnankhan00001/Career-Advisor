package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, Long> {

    Optional<PlatformSetting> findBySettingKey(String settingKey);

    boolean existsBySettingKey(String settingKey);

    List<PlatformSetting> findBySettingKeyIn(Collection<String> settingKeys);

    List<PlatformSetting> findAllByOrderByUpdatedAtDesc();
}
