package com.gateway.utils;

import net.spy.memcached.MemcachedClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.InetSocketAddress;

@Configuration
public class MemcachedConfig {
	@Value("${memcached.host}")
	private String host;
	@Value("${memcached.port}")
	private int port;

	@Bean
	public MemcachedClient memcachedClient() throws Exception {
		return new MemcachedClient(new InetSocketAddress(host, port));
	}
}
