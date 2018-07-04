package com.example.bookmarks;

import com.example.bookmarks.exceptions.BookmarkNotFoundException;
import com.example.bookmarks.exceptions.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping("/bookmarks/{userId}")
class BookmarkRestController {

  private final BookmarkRepository bookmarkRepository;
  private final AccountRepository accountRepository;

  @Autowired
  BookmarkRestController(BookmarkRepository bookmarkRepository,
                         AccountRepository accountRepository) {
    this.bookmarkRepository = bookmarkRepository;
    this.accountRepository = accountRepository;
  }

  /**
   * Serve up a collection of links at the root URI for the client to consume.
   * @return
   */
  @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
  ResourceSupport root() {
    ResourceSupport root = new ResourceSupport();

    root.add(this.accountRepository.findAll().stream()
            .map(account -> linkTo(methodOn(BookmarkRestController.class)
                    .readBookmarks(account.getUsername()))
                    .withRel(account.getUsername()))
            .collect(Collectors.toList()));

    return root;
  }/**
   * Look up a collection of {@link Bookmark}s and transform then into a set of {@link Resources}.
   *
   * @param userId
   * @return
   */
  @GetMapping(value = "/{userId}", produces = MediaTypes.HAL_JSON_VALUE)
  Resources<Resource<Bookmark>> readBookmarks(@PathVariable String userId) {

    this.validateUser(userId);

    return new Resources<>(this.bookmarkRepository
            .findByAccountUsername(userId).stream()
            .map(bookmark -> toResource(bookmark, userId))
            .collect(Collectors.toList()));
  }

  @PostMapping("/{userId}")
  ResponseEntity<?> add(@PathVariable String userId, @RequestBody Bookmark input) {

    this.validateUser(userId);

    return this.accountRepository.findByUsername(userId)
            .map(account -> ResponseEntity.created(
                    URI.create(
                            toResource(
                                    this.bookmarkRepository.save(Bookmark.from(account, input)), userId)
                                    .getLink(Link.REL_SELF).getHref()))
                    .build())
            .orElse(ResponseEntity.noContent().build());
  }

  /**
   * Find a single bookmark and transform it into a {@link Resource} of {@link Bookmark}s.
   *
   * @param userId
   * @param bookmarkId
   * @return
   */
  @GetMapping(value = "/{userId}/{bookmarkId}", produces = MediaTypes.HAL_JSON_VALUE)
  Resource<Bookmark> readBookmark(@PathVariable String userId,
                                  @PathVariable Long bookmarkId) throws BookmarkNotFoundException {
    this.validateUser(userId);

    return this.bookmarkRepository.findById(bookmarkId)
            .map(bookmark -> toResource(bookmark, userId))
            .orElseThrow(() -> new BookmarkNotFoundException(bookmarkId));
  }

  /**
   * Verify the {@literal userId} exists.
   *
   * @param userId
   */
  private void validateUser(String userId) {
    this.accountRepository
            .findByUsername(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
  }

  /**
   * Transform a {@link Bookmark} into a {@link Resource}.
   *
   * @param bookmark
   * @param userId
   * @return
   */
  private static Resource<Bookmark> toResource(Bookmark bookmark, String userId) {
    try {
      return new Resource(bookmark,

              // Create a raw link using a URI and a rel
              new Link(bookmark.getUri(), "bookmark-uri"),

              // Create a link to a the collection of bookmarks associated with the user
              linkTo(methodOn(BookmarkRestController.class).readBookmarks(userId)).withRel("bookmarks"),

              // Create a "self" link to a single bookmark
              linkTo(methodOn(BookmarkRestController.class).readBookmark(userId, bookmark.getId())).withSelfRel());
    } catch (BookmarkNotFoundException e) {
      e.printStackTrace();
    }

    return null;
  }
}