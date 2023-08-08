package com.gateway.repository;	

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.gateway.entity.Credential;
import com.gateway.response.CredentialsListAttribute;

@Repository
public interface CredentialDao extends JpaRepository<Credential, Integer> {
	Credential findByUsername(String username);
	Credential findByConsumerNumber(String consumerNumber);
	Credential findByUsernameAndChannelName(String username,String channelName);
	Credential findByUsernameAndChannelNameAndIsEnable(String username,String channelName,boolean isEnable);
	Credential findByUsernameAndPassword(String username,String password);
	Credential findByUsernameAndPasswordAndChannelName(String username,String password,String channelName);
	
	List<Credential> findByChannelName(String channelName);

    public List<Credential> findAll();
    
    @Query(value="select c.username,c.channel_name as channelName,c.is_enable as isEnable from api_gw_credentials c",nativeQuery = true)
    public List<CredentialsListAttribute> findAllUsersList();
}