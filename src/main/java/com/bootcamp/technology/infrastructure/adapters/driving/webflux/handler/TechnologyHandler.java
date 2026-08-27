package com.bootcamp.technology.infrastructure.adapters.driving.webflux.handler;

import com.bootcamp.technology.domain.api.ITechnologyServicePort;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto.TechnologyRequest;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto.TechnologyResponse;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.mapper.TechnologyDtoMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Consulta tecnologías por una lista de identificadores recibida en el
     * parámetro de consulta {@code ids} (valores separados por comas, por ejemplo
     * {@code ?ids=1,2,3}).
     *
     * <p>Pipeline reactivo de extremo a extremo, sin bloqueo: parsea los ids,
     * delega en el puerto de entrada, mapea a {@link TechnologyResponse} y responde
     * {@code 200 OK} con el arreglo JSON de las tecnologías existentes. Los ids
     * inexistentes simplemente no aparecen en el resultado; una lista de ids vacía
     * produce un arreglo vacío.
     *
     * @param request la solicitud del servidor; puede incluir el parámetro {@code ids}
     * @return un {@link Mono} que emite la respuesta {@code 200 OK} con la lista
     *         de tecnologías encontradas
     */
    public Mono<ServerResponse> findByIds(ServerRequest request) {
        List<Long> ids = request.queryParam("ids")
                .map(this::parseIds)
                .orElseGet(List::of);

        Flux<TechnologyResponse> responses = servicePort.findTechnologiesByIds(ids)
                .map(dtoMapper::toResponse);

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(responses, TechnologyResponse.class);
    }

    /**
     * Convierte el valor crudo del parámetro {@code ids} en una lista de
     * identificadores. Ignora segmentos en blanco y no numéricos para ser
     * tolerante ante entradas malformadas en un endpoint de consulta.
     *
     * @param raw valor del parámetro (por ejemplo {@code "1,2,3"}).
     * @return lista de identificadores válidos parseados; nunca {@code null}.
     */
    private List<Long> parseIds(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(segment -> segment.matches("\\d+"))
                .map(Long::valueOf)
                .distinct()
                .collect(Collectors.toList());
    }
}
