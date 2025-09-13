package dev.filipe.TODOLambdaJava;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HandlerTest {

    @Mock
    private  Context context;

    @Mock
    private LambdaLogger logger;

    private Handler handler;

    @BeforeEach
    void setUp() {
        handler = new Handler();

        when(context.getLogger()).thenReturn(logger);

    }

    @Test
    void testHandleRequest() {
        Object input = new Object();
        String expectedMessage = "Hello do Lambda! É só o começo...";
        String result = handler.handleRequest(input, context);
        assertEquals(expectedMessage, result);

        verify(logger).log("Input: " + input);
    }

}
