package uk.gov.hmcts.reform.authorisation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.authorisation.config.IntegrationTestInitializer;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

@ActiveProfiles("wiremock")
@AutoConfigureWireMock
@ContextConfiguration(initializers = IntegrationTestInitializer.class)
@ComponentScan
@EnableAutoConfiguration
@EnableConfigurationProperties()
@TestPropertySource(properties = {
        "idam.s2s-authorised.services=service1,service1",
})
@RunWith(SpringRunner.class)
@SpringBootTest(classes = IntegrationTestInitializer.class)
public class ServiceAuthorisationApiTest {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @Autowired
    private ServiceAuthorisationApi s2sApi;

    @Autowired
    private ServiceAuthFilter serviceAuthFilter;

    private FilterChain filterChain;

    private HttpServletRequest httpServletRequest;

    @Before
    public  void before() {
        Assert.assertNotNull(serviceAuthFilter);
        filterChain = spy(FilterChain.class);
        httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getHeader(SERVICE_AUTHORIZATION)).thenReturn("token");
    }

    @Test
    public void should_get_service_name_providing_valid_token() {
        AuthTokenValidator validator = new ServiceAuthTokenValidator(s2sApi);
        givenThat(get("/details").willReturn(status(OK.value()).withBody("service")));
        assertThat(validator.getServiceName("token")).isEqualTo("service");
    }

    @Test
    public void should_pass_serviceAuthFilter_with_authorized_access() throws ServletException, IOException {
        givenThat(get("/details").willReturn(status(OK.value()).withBody("service1")));
        serviceAuthFilter.doFilter(httpServletRequest, mock(HttpServletResponse.class), filterChain);
        Mockito.verify(filterChain).doFilter(Matchers.any(), Matchers.any());
    }

    @Test
    public void should_fail_serviceAuthFilter_with_Unauthorized_access() throws ServletException, IOException {
        givenThat(get("/details").willReturn(status(OK.value()).withBody("service")));
        serviceAuthFilter.doFilter(httpServletRequest, mock(HttpServletResponse.class), filterChain);
        Mockito.verify(filterChain, never()).doFilter(Matchers.any(), Matchers.any());
    }

}
