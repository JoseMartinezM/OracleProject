package com.springboot.MyTodoList;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.TelegramBotsApi;

@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public TelegramBotsApi telegramBotsApi() {
        // Devuelve un mock de TelegramBotsApi para evitar la inicialización real
        return Mockito.mock(TelegramBotsApi.class);
    }
}
