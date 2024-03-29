package com.leoncarraro.library_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leoncarraro.library_api.dto.BookRequestCreate;
import com.leoncarraro.library_api.dto.BookRequestUpdate;
import com.leoncarraro.library_api.dto.BookResponse;
import com.leoncarraro.library_api.service.BookService;
import com.leoncarraro.library_api.service.exception.ExistingBookException;
import com.leoncarraro.library_api.service.exception.ResourceNotFoundException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

@ActiveProfiles(value = "test")
@WebMvcTest(controllers = {BookController.class})
@AutoConfigureMockMvc
public class BookControllerTest {

    private static final String BOOK_URI = "/api/books";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Test
    @DisplayName(value = "Should return Created status with response body and correct Location header")
    public void shouldReturnCreatedStatus_WhenSaveValidBook() throws Exception {
        BookRequestCreate bookRequest = BookRequestCreate.builder()
                .title("Title").author("Author").isbn("ISBN").build();
        BookResponse bookResponse = BookResponse.builder()
                .id(1L).title("Title").author("Author").isbn("ISBN").build();

        Mockito.when(bookService.create(Mockito.any(BookRequestCreate.class))).thenReturn(bookResponse);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(BOOK_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(bookRequest));

        mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("title").value("Title"))
                .andExpect(MockMvcResultMatchers.jsonPath("author").value("Author"))
                .andExpect(MockMvcResultMatchers.jsonPath("isbn").value("ISBN"))
                .andExpect(MockMvcResultMatchers.header().string("Location", "http://localhost/api/books/1"));
    }

    @Test
    @DisplayName(value = "Should throw a MethodArgumentNotValidException with errors message " +
            "when try to create one Book with invalid properties")
    public void shouldThrowAnException_WhenCreateBookWithInvalidProperties() throws Exception {
        BookRequestCreate bookRequest = BookRequestCreate.builder()
                .title(null).author(null).isbn(null).build();

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(BOOK_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(bookRequest));

        mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("errors", Matchers.hasSize(3)));
    }

    @Test
    @DisplayName(value = "Should return a Bad Request status with error message " +
            "when try to create a Book with existing ISBN")
    public void shouldReturnBadRequestStatus_WhenSaveBookWithExistingIsbn() throws Exception {
        BookRequestCreate bookRequest = BookRequestCreate.builder()
                .title("Title").author("Author").isbn("ISBN").build();

        Mockito.when(bookService.create(Mockito.any(BookRequestCreate.class)))
                .thenThrow(new ExistingBookException("ISBN: ISBN already registered!"));

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(BOOK_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(bookRequest));

        mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("errors", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("errors[0]").value("ISBN: ISBN already registered!"));
    }

    @Test
    @DisplayName(value = "Should return a Ok status with response body when get a Book information correctly")
    public void shouldReturnOkStatus_WhenGetBookInformation() throws Exception {
        BookResponse bookResponse = BookResponse.builder()
                .id(1L).author("Author").title("Title").isbn("ISBN").build();

        Mockito.when(bookService.findById(1L)).thenReturn(bookResponse);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(BOOK_URI + "/1")
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("title").value("Title"))
                .andExpect(MockMvcResultMatchers.jsonPath("author").value("Author"))
                .andExpect(MockMvcResultMatchers.jsonPath("isbn").value("ISBN"));
    }

    @Test
    @DisplayName(value = "Should return a Not Found status with error message " +
            "when get a Book information with no existent ID")
    public void shouldReturnNotFoundStatus_WhenGetBookInformationWithNoExistentId() throws Exception {
        Mockito.when(bookService.findById(1L)).thenThrow(new ResourceNotFoundException("Book 1 not found!"));

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(BOOK_URI + "/1")
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("errors", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("errors[0]").value("Book 1 not found!"));
    }

    @Test
    @DisplayName(value = "Should return a No Content status when delete a Book correctly")
    public void shouldReturnNoContentStatus_WhenDeleteBook() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .delete(BOOK_URI + "/1");

        mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    @DisplayName(value = "Should return a Not Found status with error message when delete a Book with no existent ID")
    public void shouldReturnNotFoundStatus_WhenDeleteBookWithNoExistentId() throws Exception {
        Mockito.doThrow(new ResourceNotFoundException("Book 1 not found!")).when(bookService).delete(1L);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .delete(BOOK_URI + "/1");

        mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("errors", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("errors[0]").value("Book 1 not found!"));
    }

    @Test
    @DisplayName(value = "Should return a Ok status with response body when update the Book informations correctly")
    public void shouldReturnOkStatus_WhenUpdateBook() throws Exception {
        BookRequestUpdate bookRequest = BookRequestUpdate.builder()
                .author("Author Update").title("Title Update").build();
        BookResponse bookResponse = BookResponse.builder()
                .id(1L).author("Author Update").title("Title Update").isbn("ISBN").build();

        Mockito.when(bookService.update(Mockito.eq(1L), Mockito.any(BookRequestUpdate.class))).thenReturn(bookResponse);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .put(BOOK_URI + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(bookRequest));

        mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("title").value("Title Update"))
                .andExpect(MockMvcResultMatchers.jsonPath("author").value("Author Update"))
                .andExpect(MockMvcResultMatchers.jsonPath("isbn").value("ISBN"));
    }

    @Test
    @DisplayName(value = "Should return a Not Found status with error message when update a Book with no existent ID")
    public void shouldReturnNotFoundStatus_WhenUpdateBookWithNoExistentId() throws Exception {
        BookRequestUpdate bookRequest = BookRequestUpdate.builder()
                .author("Author Update").title("Title Update").build();

        Mockito.when(bookService.update(Mockito.eq(1L), Mockito.any(BookRequestUpdate.class)))
                .thenThrow(new ResourceNotFoundException("Book 1 not found!"));

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .put(BOOK_URI + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(bookRequest));

        mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("errors", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("errors[0]").value("Book 1 not found!"));
    }

    @Test
    @DisplayName(value = "Should throw a MethodArgumentNotValidException with errors message " +
            "when try to update one Book with invalid properties")
    public void shouldThrowAnException_WhenUpdateBookWithInvalidProperties() throws Exception {
        BookRequestUpdate bookRequest = BookRequestUpdate.builder()
                .title(null).author(null).build();

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .put(BOOK_URI + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(bookRequest));

        mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("errors", Matchers.hasSize(2)));
        Mockito.verify(bookService, Mockito.never()).update(Mockito.anyLong(), Mockito.any(BookRequestUpdate.class));
    }

    @Test
    @DisplayName(value = "Should return a Ok status with response body when request a pagination of Books")
    public void shouldReturnOkStatus_WhenRequestPaginationOfBooks() throws Exception {
        BookResponse bookResponse = BookResponse.builder()
                .id(1L).author("Author").isbn("ISBN").title("Title").build();

        Mockito.when(bookService.findWithFilter(
                Mockito.anyString(), Mockito.anyString(), Mockito.any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(bookResponse), PageRequest.of(0, 12), 1L));

        String queryString = String.format("?title=%s&author=%s&page=0&size=12", "Author", "Title");
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(BOOK_URI + queryString)
                .accept(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("content", Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("totalElements").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("pageable.pageSize").value(12))
                .andExpect(MockMvcResultMatchers.jsonPath("pageable.pageNumber").value(0));
    }

}
