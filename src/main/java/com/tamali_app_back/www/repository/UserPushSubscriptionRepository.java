package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.UserPushSubscription;
import com.tamali_app_back.www.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPushSubscriptionRepository extends JpaRepository<UserPushSubscription, UUID> {

    Optional<UserPushSubscription> findByEndpoint(String endpoint);

    List<UserPushSubscription> findByUser_Id(UUID userId);

    List<UserPushSubscription> findByUser_IdIn(Collection<UUID> userIds);

    @Query("SELECT DISTINCT ups FROM UserPushSubscription ups JOIN ups.user u JOIN u.roles r WHERE r.type = :roleType")
    List<UserPushSubscription> findAllByUserHavingRole(@Param("roleType") RoleType roleType);

    void deleteByEndpoint(String endpoint);

    @Modifying
    @Query("DELETE FROM UserPushSubscription u WHERE u.endpoint = :endpoint AND u.user.id = :userId")
    int deleteByEndpointAndUserId(@Param("endpoint") String endpoint, @Param("userId") UUID userId);
}
