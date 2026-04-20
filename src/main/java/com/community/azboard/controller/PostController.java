package com.community.azboard.controller;

import com.community.azboard.domain.Post;
import com.community.azboard.dto.PostCreateRequest;
import com.community.azboard.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @PostMapping
    public Long create(@Valid @RequestBody PostCreateRequest request) {
        return postService.savePost(request.getTitle(), request.getContent(), request.getAuthor());
    }

    @GetMapping
    public List<Post> list() {
        return postService.findPosts();
    }
}
