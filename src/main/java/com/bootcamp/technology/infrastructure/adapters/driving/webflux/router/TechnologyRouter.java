package com.bootcamp.technology.infrastructure.adapters.driving.webflux.router;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

import com.bootcamp.technology.domain.api.ITechnologyServicePort;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto.ErrorResponse;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto.TechnologyRequest;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.dto.TechnologyResponse;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.handler.TechnologyHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Router de la capa driving (WebFlux funcional) que declara las rutas del
 * recurso {@code technologies} y las asocia al {@link TechnologyHandler}.
 *
 * <p>Los endpoints funcionales ({@code RouterFunction}) no exponen su contrato
 * automáticamente a springdoc como lo hacen los {@code @RestController}. Por ello
 * la documentación OpenAPI del endpoint se declara de forma explícita con las
 * anotaciones {@link RouterOperations}/{@link RouterOperation} sobre el método
 * que produce el bean {@code RouterFunction}, describiendo el esquema de la
 * solicitud, el de la respuesta {@code 201} y los errores {@code 400}/{@code 409}
 * (Requerimiento 6.1).
 */
@Configuration
public class TechnologyRouter {

    private static final String TECHNOLOGIES_PATH = "/api/v1/technologies";

    /**
     * Declara la ruta {@code POST /api/v1/technologies} (que acepta
     * {@code application/json}) y la delega en {@link TechnologyHandler#register}.
     *
     * @param handler handler que procesa el registro de tecnologías
     * @return la {@link RouterFunction} con la ruta de registro configurada
     */
    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = TECHNOLOGIES_PATH,
                    method = RequestMethod.POST,
                    beanClass = ITechnologyServicePort.class,
                    beanMethod = "registerTechnology",
                    operation = @Operation(
                            operationId = "registerTechnology",
                            summary = "Registra una nueva tecnología",
                            description = "Valida obligatoriedad y longitudes (nombre 1-50, "
                                    + "descripción 1-90) y unicidad del nombre "
                                    + "(case-insensitive), y persiste la tecnología.",
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = TechnologyRequest.class))),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "201",
                                            description = "Tecnología registrada correctamente",
                                            content = @Content(
                                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                    schema = @Schema(implementation = TechnologyResponse.class))),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Datos inválidos (nombre/descripción "
                                                    + "obligatorios o exceden la longitud máxima)",
                                            content = @Content(
                                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                    schema = @Schema(implementation = ErrorResponse.class))),
                                    @ApiResponse(
                                            responseCode = "409",
                                            description = "El nombre de la tecnología ya está registrado",
                                            content = @Content(
                                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                    schema = @Schema(implementation = ErrorResponse.class)))
                            })),
            @RouterOperation(
                    path = TECHNOLOGIES_PATH,
                    method = RequestMethod.GET,
                    beanClass = ITechnologyServicePort.class,
                    beanMethod = "findTechnologiesByIds",
                    operation = @Operation(
                            operationId = "findTechnologiesByIds",
                            summary = "Consulta tecnologías por identificadores",
                            description = "Recupera las tecnologías cuyos identificadores se "
                                    + "indican en el parámetro de consulta 'ids' (separados por "
                                    + "comas, por ejemplo ?ids=1,2,3). Solo se devuelven las "
                                    + "tecnologías existentes; los identificadores inexistentes "
                                    + "se omiten. Pensado para consumo entre microservicios.",
                            parameters = {
                                    @Parameter(
                                            in = ParameterIn.QUERY,
                                            name = "ids",
                                            description = "Identificadores de tecnología separados por comas",
                                            example = "1,2,3")
                            },
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Lista de tecnologías encontradas (puede estar vacía)",
                                            content = @Content(
                                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                    array = @ArraySchema(
                                                            schema = @Schema(implementation = TechnologyResponse.class))))
                            }))
    })
    public RouterFunction<ServerResponse> technologyRoutes(TechnologyHandler handler) {
        return RouterFunctions.route()
                .POST(TECHNOLOGIES_PATH, accept(MediaType.APPLICATION_JSON), handler::register)
                .GET(TECHNOLOGIES_PATH, accept(MediaType.APPLICATION_JSON), handler::findByIds)
                .build();
    }
}
