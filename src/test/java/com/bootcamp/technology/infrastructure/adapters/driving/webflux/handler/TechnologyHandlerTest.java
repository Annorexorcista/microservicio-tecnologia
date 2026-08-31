package com.bootcamp.technology.infrastructure.adapters.driving.webflux.handler;

import com.bootcamp.technology.domain.api.ITechnologyServicePort;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.exception.InvalidIdsQueryException;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.exception.RequestErrorCode;
import com.bootcamp.technology.infrastructure.adapters.driving.webflux.mapper.TechnologyDtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnologyHandlerTest {

    @Mock
    private ITechnologyServicePort servicePort;

    @Mock
    private ServerRequest request;

    private TechnologyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TechnologyHandler(servicePort, new TechnologyDtoMapper());
    }

    @Test
    void findByIds_withoutIds_rejectsRequestBeforeCallingUseCase() {
        when(request.queryParam("ids")).thenReturn(Optional.empty());

        StepVerifier.create(handler.findByIds(request))
                .expectErrorSatisfies(error -> assertInvalidIdsError(
                        error, RequestErrorCode.IDS_REQUIRED))
                .verify();

        verifyNoInteractions(servicePort);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1,abc,3",
            "1,-2,3",
            "1,0,3",
            "1,,3",
            "1,2,",
            "9223372036854775808"
    })
    void findByIds_withInvalidIds_rejectsRequestBeforeCallingUseCase(String rawIds) {
        when(request.queryParam("ids")).thenReturn(Optional.of(rawIds));

        StepVerifier.create(handler.findByIds(request))
                .expectError(InvalidIdsQueryException.class)
                .verify();

        verifyNoInteractions(servicePort);
    }

    @Test
    void findByIds_withValidIds_removesDuplicatesAndCallsUseCase() {
        when(request.queryParam("ids")).thenReturn(Optional.of(" 3,1,3,2 "));
        when(servicePort.findTechnologiesByIds(anyCollection())).thenReturn(Flux.empty());

        StepVerifier.create(handler.findByIds(request))
                .assertNext(response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(servicePort).findTechnologiesByIds(captor.capture());
        assertThat(captor.getValue()).containsExactly(3L, 1L, 2L);
    }

    @Test
    void deleteByIds_withInvalidIds_doesNotCallUseCase() {
        when(request.queryParam("ids")).thenReturn(Optional.of("1,abc"));

        StepVerifier.create(handler.deleteByIds(request))
                .expectErrorSatisfies(error -> assertInvalidIdsError(
                        error, RequestErrorCode.ID_NOT_NUMERIC))
                .verify();

        verify(servicePort, never()).deleteTechnologiesByIds(anyCollection());
    }

    @Test
    void deleteByIds_withValidIds_removesDuplicatesAndReturnsNoContent() {
        when(request.queryParam("ids")).thenReturn(Optional.of("2,1,2"));
        when(servicePort.deleteTechnologiesByIds(anyCollection())).thenReturn(Mono.empty());

        StepVerifier.create(handler.deleteByIds(request))
                .assertNext(response -> assertThat(response.statusCode())
                        .isEqualTo(HttpStatus.NO_CONTENT))
                .verifyComplete();

        verify(servicePort).deleteTechnologiesByIds(List.of(2L, 1L));
    }

    private void assertInvalidIdsError(Throwable error, RequestErrorCode expectedCode) {
        assertThat(error).isInstanceOf(InvalidIdsQueryException.class);
        InvalidIdsQueryException exception = (InvalidIdsQueryException) error;
        assertThat(exception.getCode()).isEqualTo(expectedCode);
    }
}
