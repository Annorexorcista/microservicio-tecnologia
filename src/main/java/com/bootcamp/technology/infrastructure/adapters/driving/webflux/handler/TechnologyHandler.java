package com.bootcamp.technology.infrastructure.adapters.driving.webflux.handler;

import com.bootcamp.technology.domain.api.ITechnologyServicePort;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto.TechnologyRequest;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.mapper.TechnologyDtoMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler de la capa driving (WebFlux funcional) para el registro de tecnologías.
 *
 * <p>Compone un pipeline reactivo de extremo a extremo, sin llamadas bloqueantes
 * ({@code .block()}): deserializa el cuerpo de la solicitud a {@link TechnologyRequest},
 * lo mapea al modelo de dominio, delega en el puerto de entrada
 * {@link ITechnologyServicePort}, mapea el resultado a DTO de respuesta y construye
 * la respuesta {@code 201 Created}.
 *
 * <p>El manejo de errores no ocurre aquí: cualquier {@code Mono.error} emitido por el
 * caso de uso (validación, unicidad) o por la deserialización fluye por el pipeline y
 * lo traduce el handler global de errores.
 *
 * <p>Se construye como bean en {@code BeanConfiguration}, por lo que la clase no lleva
 * la anotación {@code @Component} (evitando la creación de un bean duplicado). Las
 * dependencias se inyectan por constructor.
 */
public class TechnologyHandler {

    private final ITechnologyServicePort servicePort;
    private final TechnologyDtoMapper dtoMapper;

    /**
     * Crea el handler con sus colaboradores.
     *
     * @param servicePort puerto de entrada del dominio que ejecuta el registro
     * @param dtoMapper   mapper entre DTOs de la capa web y el modelo de dominio
     */
    public TechnologyHandler(ITechnologyServicePort servicePort, TechnologyDtoMapper dtoMapper) {
        this.servicePort = servicePort;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Registra una tecnología a partir de la solicitud HTTP.
     *
     * <p>Pipeline reactivo: {@code bodyToMono -> map(toDomain) ->
     * flatMap(registerTechnology) -> map(toResponse) -> flatMap(ServerResponse 201)}.
     * Los errores se propagan hacia el handler global; aquí no se capturan.
     *
     * @param request la solicitud del servidor con el cuerpo {@link TechnologyRequest}
     * @return un {@link Mono} que emite la respuesta {@code 201 Created} con el DTO
     *         de la tecnología creada, o propaga el error correspondiente
     */
    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(TechnologyRequest.class)
                .map(dtoMapper::toDomain)
                .flatMap(servicePort::registerTechnology)
                .map(dtoMapper::toResponse)
                .flatMap(response -> ServerResponse
                        .status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }
}
