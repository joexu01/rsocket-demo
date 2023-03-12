package org.example.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class MathService {
    public Flux<String> doubleInteger(Flux<String> request) {
        return request
                .map(s -> {
                    System.out.println("received " + s);
                    int i = Integer.parseInt(s);
                    return String.valueOf(i * 2);
                });
    }

}