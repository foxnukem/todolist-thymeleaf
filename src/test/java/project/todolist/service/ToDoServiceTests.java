package project.todolist.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
import project.todolist.exception.NullReferenceEntityException;
import project.todolist.exception.UserIsOwnerOfThisToDoException;
import project.todolist.model.ToDo;
import project.todolist.model.User;
import project.todolist.repository.ToDoRepository;
import project.todolist.repository.UserRepository;
import project.todolist.service.impl.ToDoServiceImpl;

import javax.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ToDoServiceTests {
    @Mock
    private ToDoRepository toDoRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ToDoServiceImpl toDoService;

    private ToDo expected;
    private User user;
    private final long todoId = 1L;
    private final long userId = 2L;

    @BeforeEach
    public void setUp() {
        expected = new ToDo();
        expected.setId(todoId);
        String title = "title";
        expected.setTitle(title);
        user = new User();
        user.setId(userId);
        expected.setOwner(user);
    }

    @Test
    @DisplayName("save(todo) throws NullReferenceEntityException when passing null")
    void saveNullThrowsException() {
        var exception = assertThrows(NullReferenceEntityException.class, () -> toDoService.save(null));
        assertEquals("Given ToDo cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("save(todo) saves new correct ToDo")
    void saveNonExistingToDo() {
        when(toDoRepository.save(any(ToDo.class))).thenReturn(expected);

        var actual = toDoService.save(expected);

        verify(toDoRepository).save(any(ToDo.class));
        assertEquals(expected, actual);
        assertEquals(expected.getId(), actual.getId());
    }

    @Test
    @DisplayName("save(todo) updates existing ToDo")
    void updateExistingToDo() {
        when(toDoRepository.save(any(ToDo.class))).thenReturn(expected);

        var actual = toDoService.save(expected);

        verify(toDoRepository).save(any(ToDo.class));
        assertEquals(expected, actual);
        assertEquals(expected.getId(), actual.getId());
    }

    @Test
    @DisplayName("readById(id) returns correct ToDo by its id")
    void readExistingToDoById() {
        when(toDoRepository.findById(anyLong())).thenReturn(Optional.of(expected));

        var actual = toDoService.readById(todoId);

        verify(toDoRepository).findById(anyLong());
        assertEquals(expected, actual);
        assertEquals(expected.getId(), actual.getId());
    }

    @Test
    @DisplayName("readById(id) throws an EntityNotFoundException when ToDo with given id was not found")
    void readNonExistingToDoById() {
        assertThrows(EntityNotFoundException.class, () -> toDoService.readById(1L));
    }

    @Test
    @Transactional
    @DisplayName("delete(id) removes ToDo by its id")
    void deleteExistingToDoById() {
        when(toDoRepository.findById(anyLong())).thenReturn(Optional.of(expected));

        toDoService.delete(todoId);

        verify(toDoRepository).findById(anyLong());
        verify(toDoRepository).delete(any(ToDo.class));
    }

    @Test
    @DisplayName("delete(id) throws an EntityNotFoundException when Task with given id was not found")
    void deleteNonExistingToDoById() {
        when(toDoRepository.findById(anyLong())).thenReturn(Optional.empty());

        var exception = assertThrows(EntityNotFoundException.class, () -> toDoService.delete(todoId));

        assertEquals("ToDo (id=" + todoId + ") was not found", exception.getMessage());
        verify(toDoRepository).findById(anyLong());
        verify(toDoRepository, times(0)).delete(any(ToDo.class));
    }

    @Test
    @DisplayName("getAll() returns all created ToDos")
    void getAllToDos() {
        when(toDoRepository.findAll()).thenReturn(List.of(new ToDo(), new ToDo(), new ToDo()));

        var actual = toDoService.getAll();

        assertEquals(3, actual.size());
        verify(toDoRepository).findAll();
    }

    @Test
    @DisplayName("getAllToDosOfUser(id) returns all created ToDo of existing User")
    void getAllToDosOfExistingUser() {
        when(toDoRepository.getToDosByUserId(anyLong())).thenReturn(Collections.emptyList());

        var actual = toDoService.getAllToDoOfUser(userId);

        assertEquals(0, actual.size());
        verify(toDoRepository).getToDosByUserId(anyLong());
    }

    @Test
    @DisplayName("getAllToDosOfUser(id) throws an EntityNotFoundException when User with given id was not found")
    void getAllToDosOfNonExistingUser() {
        when(toDoService.getAllToDoOfUser(anyLong())).thenThrow(new EntityNotFoundException());
        assertThrows(EntityNotFoundException.class, () -> toDoService.getAllToDoOfUser(anyLong()));
    }

    @Test
    @DisplayName("getAllToDosOfUser(id) returns empty list of ToDos if User has no created ToDos")
    void getEmptyListOfToDosWhenUserHasNoCreatedToDo() {
        var expected = Collections.emptyList();

        when(toDoRepository.getToDosByUserId(anyLong())).thenReturn(Collections.emptyList());

        var actual = toDoService.getAllToDoOfUser(userId);

        verify(toDoRepository).getToDosByUserId(anyLong());
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("addCollaborator(todoId, userid) throws UserIsOwnerOfThisToDoException when given User is an owner of given ToDo")
    void addOwnerAsCollaborator() {
        when(toDoRepository.findById(anyLong())).thenReturn(Optional.of(expected));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(expected.getOwner()));

        assertThrows(UserIsOwnerOfThisToDoException.class, () -> toDoService.addCollaborator(expected.getId(), expected.getOwner().getId()));
    }

    @Test
    @DisplayName("addCollaborator(todoId, userid) adds new collaborator to ToDo ")
    void addCollaborator() {
        User collaborator = new User();
        when(toDoRepository.findById(anyLong())).thenReturn(Optional.of(expected));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(collaborator));

        toDoService.addCollaborator(expected.getId(), collaborator.getId());
        assertTrue(toDoService.readById(expected.getId()).getCollaborators().contains(collaborator));
    }

    @Test
    @DisplayName("addCollaborator(todoId, userid) throws an EntityNotFoundException when ToDo with given id was not found")
    void addCollaboratorNotFoundToDo() {
        assertThrows(EntityNotFoundException.class, () -> toDoService.addCollaborator(expected.getId(), user.getId()));
    }

    @Test
    @DisplayName("addCollaborator(todoId, userid) throws an EntityNotFoundException when User with given id was not found")
    void addCollaboratorNotFoundUser() {
        when(toDoRepository.findById(anyLong())).thenReturn(Optional.of(expected));
        assertThrows(EntityNotFoundException.class, () -> toDoService.addCollaborator(expected.getId(), user.getId()));
    }

    @Test
    @DisplayName("removeCollaborator(todoId, userid) throws UserIsOwnerOfThisToDoException when given User is an owner of given ToDo")
    void removeOwnerFromCollaborators() {
        when(toDoRepository.findById(anyLong())).thenReturn(Optional.of(expected));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(expected.getOwner()));

        assertThrows(UserIsOwnerOfThisToDoException.class, () -> toDoService.removeCollaborator(expected.getId(), expected.getOwner().getId()));
    }

    @Test
    @DisplayName("removeCollaborator(todoId, userid) throws an EntityNotFoundException when ToDo with given id was not found")
    void removeCollaboratorNotFoundToDo() {
        assertThrows(EntityNotFoundException.class, () -> toDoService.removeCollaborator(expected.getId(), user.getId()));
    }

    @Test
    @DisplayName("removeCollaborator(todoId, userid) throws an EntityNotFoundException when User with given id was not found")
    void removeNotFoundUser() {
        when(toDoRepository.findById(anyLong())).thenReturn(Optional.of(expected));
        assertThrows(EntityNotFoundException.class, () -> toDoService.removeCollaborator(expected.getId(), user.getId()));
    }

    @Test
    @DisplayName("removeCollaborator(todoId, userid) removes collaborator")
    void removeCollaborator() {
        User collaborator = new User();
        when(toDoRepository.findById(anyLong())).thenReturn(Optional.of(expected));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(collaborator));

        toDoService.removeCollaborator(expected.getId(), collaborator.getId());
        assertFalse(toDoService.readById(expected.getId()).getCollaborators().contains(collaborator));
    }
}
