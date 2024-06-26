package com.thiago.libraryapi.service;

import com.thiago.libraryapi.excepition.BusinessExcepition;
import com.thiago.libraryapi.model.entity.Book;
import com.thiago.libraryapi.model.repository.BookRepository;
import com.thiago.libraryapi.service.impl.BookServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class BookServiceTest {

    BookService service;

    @MockBean
    BookRepository repository;

    @BeforeEach
    public void setUp() {
        this.service = new BookServiceImpl(repository);
    }

    @Test
    @DisplayName("Deve salvar um livro")
    public void savaBookTest() {
        //cenario
        Book book = createValidBook();
        Mockito.when( repository.existsByIsbn(Mockito.anyString()) ).thenReturn(false);

        Mockito.when( repository.save(book) ).thenReturn(
                Book.builder().id(11L)
                        .isbn("123")
                        .author("Fulano")
                        .title("As aventuras").build()
        );

        //execucao
        Book savedBook = service.save(book);

        //verificacao
        assertThat(savedBook.getId()).isNotNull();
        assertThat(savedBook.getIsbn()).isEqualTo("123");
        assertThat(savedBook.getTitle()).isEqualTo("As aventuras");
        assertThat(savedBook.getAuthor()).isEqualTo("Fulano");
    }

    @Test
    @DisplayName("Deve lançar erro de negócio ao tentar salvar um livro com isbn duplicado")
    public void shouldNotSaveABookWithDuplicatedISBN() {
        //cenario
        Book book = createValidBook();
        Mockito.when( repository.existsByIsbn(Mockito.anyString()) ).thenReturn(true);

        //execução
        Throwable excepition = Assertions.catchThrowable(() -> service.save(book) );

        //verificação
        assertThat(excepition)
                .isInstanceOf(BusinessExcepition.class)
                .hasMessage("Isbn já cadastrado.");

        Mockito.verify(repository, Mockito.never()).save(book);
    }

    @Test
    @DisplayName("Deve obter um livro por Id")
    public void getByIdTest() {
        //cenario
        Long id = 1l;
        Book book = createValidBook();
        book.setId(id);
        Mockito.when(repository.findById(id)).thenReturn(Optional.of(book));

        //execução
        Optional<Book> foundBook = service.getById(id);

        //verificação
        assertThat( foundBook.isPresent() ).isTrue();
        assertThat( foundBook.get().getId() ).isEqualTo(id);
        assertThat( foundBook.get().getAuthor() ).isEqualTo(book.getAuthor());
        assertThat( foundBook.get().getIsbn() ).isEqualTo(book.getIsbn());
        assertThat( foundBook.get().getTitle() ).isEqualTo(book.getTitle());
    }

    @Test
    @DisplayName("Deve retornar vazio ao obter um livro por Id quando ele não existe na base")
    public void bookNotFoundByIdTest() {
        //cenario
        Long id = 1l;
        Mockito.when(repository.findById(id)).thenReturn(Optional.empty());

        //execução
        Optional<Book> foundBook = service.getById(id);

        //verificação
        assertThat( foundBook.isPresent() ).isFalse();
    }

    @Test
    @DisplayName("Deve deletar um livro")
    public void deleteBookTest() {
        //cenario
        Book book = Book.builder().id(1l).build();

        //execução
        org.junit.jupiter.api.Assertions.assertDoesNotThrow( () -> service.delete(book) );

        //verificação
        Mockito.verify( repository, Mockito.times(1) ).delete(book);
    }

    @Test
    @DisplayName("Deve ocorrer um erro ao tentar deletar um livro inexistente")
    public void deleteInvalidBookTest() {
        //cenario
        Book book = new Book();

        //execução
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.delete(book)
        );

        //verificação
        Mockito.verify( repository, Mockito.never() ).delete(book);
    }

    @Test
    @DisplayName("Deve atualisar um livro")
    public void updateBookTest() {
        //cenario
        Long id = 1l;

        //Livro a atualizar
        Book updatingBook = Book.builder().id(id).build();

        //simulação
        Book updatedBook = createValidBook();
        updatedBook.setId(id);
        Mockito.when(repository.save(updatingBook)).thenReturn(updatedBook);

        //execução
        Book book = service.update(updatingBook);

        //verificação
        assertThat(book.getId()).isEqualTo(updatedBook.getId());
        assertThat(book.getTitle()).isEqualTo(updatedBook.getTitle());
        assertThat(book.getIsbn()).isEqualTo(updatedBook.getIsbn());
        assertThat(book.getAuthor()).isEqualTo(updatedBook.getAuthor());
    }

    @Test
    @DisplayName("Deve ocorrer um erro ao tentar atualizar um livro inexistente")
    public void updateInvalidBookTest() {
        //cenario
        Book book = new Book();

        //execução
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.update(book)
        );

        //verificação
        Mockito.verify( repository, Mockito.never() ).save(book);
    }

    @Test
    @DisplayName("Deve filtrar livros pelas propriedades")
    public void findBookTest() {
        //cenario
        Book book = createValidBook();

        PageRequest pageRequest = PageRequest.of(0,10);

        List<Book> lista = Arrays.asList(book);
        Page<Book> page = new PageImpl<Book>(lista, pageRequest, 1);
        Mockito.when(repository.findAll(Mockito.any(Example.class), Mockito.any(PageRequest.class)))
                .thenReturn(page);

        //execução
        Page<Book> result = service.find(book, pageRequest);

        //verificação
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).isEqualTo(lista);
        assertThat(result.getPageable().getPageNumber()).isEqualTo(0);
        assertThat(result.getPageable().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("Deve obter um livro pelo isbn")
    public void getBookByIsbnTeste() {
        String isbn = "1230";
        Mockito.when( repository.findByIsbn(isbn) ).thenReturn( Optional.of( Book.builder().id(1l).isbn(isbn).build() ) );

        Optional<Book> book = service.getBookByIsbn(isbn);

        assertThat(book.isPresent()).isTrue();
        assertThat(book.get().getId()).isEqualTo(1l);
        assertThat(book.get().getIsbn()).isEqualTo(isbn);

        Mockito.verify(repository, Mockito.times(1)).findByIsbn(isbn);
    }

    private static Book createValidBook() {
        return Book.builder().isbn("123").author("Fulano").title("As aventuras").build();
    }
}




















