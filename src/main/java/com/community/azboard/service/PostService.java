package com.community.azboard.service;

import com.community.azboard.domain.Post;
import com.community.azboard.domain.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    public Long savePost(String title, String content, String author) {
        Post post = Post.builder()
                .title(title)
                .content(content)
                .author(author)
                .build();

        postRepository.save(post);
        Long savedId = post.getId();

        kafkaProducerService.sendNewPostNotification(savedId, author, title);

        return savedId;
    }

    public List<Post> findPosts() {
        return postRepository.findAll();
    }
}
