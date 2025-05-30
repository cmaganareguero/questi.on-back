package com.top.application.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenFilter extends AbstractGatewayFilterFactory<Object> {

    @Autowired
    private JwtTokenVerifier jwtTokenVerifier;

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            // Obtener el token JWT de la solicitud
            String token = extractTokenFromRequest(exchange.getRequest().getHeaders().getFirst("Authorization"));

            // Verificar el token JWT
            if (token == null || !jwtTokenVerifier.verifyToken(token)) {
                // Si el token no es válido, devuelve un error de autorización
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Si el token es válido, continua con la cadena de filtros
            return chain.filter(exchange);
        };
    }

    private String extractTokenFromRequest(String authHeader) {
        // Extracción del token de Authorization header
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
