package com.example.bookmarks;

import com.example.bookmarks.exceptions.BookmarkNotFoundException;
import com.example.bookmarks.exceptions.UserNotFoundException;
import org.springframework.hateoas.VndErrors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
class BookmarkControllerAdvice {

  @ResponseBody
  @ExceptionHandler(UserNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  VndErrors userNotFoundExceptionHandler(UserNotFoundException ex) {
    return new VndErrors("error", ex.getMessage());
  }

  @ResponseBody
  @ExceptionHandler(BookmarkNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  VndErrors bookmarkNotFoundExceptionHandler(BookmarkNotFoundException ex) {
    return new VndErrors("error", ex.getMessage());
  }
}