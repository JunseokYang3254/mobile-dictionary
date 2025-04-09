package com.kimtaeyang.mobidic.service;

import com.kimtaeyang.mobidic.dto.AddWordDto;
import com.kimtaeyang.mobidic.dto.WordDetailDto;
import com.kimtaeyang.mobidic.dto.WordDto;
import com.kimtaeyang.mobidic.entity.*;
import com.kimtaeyang.mobidic.exception.ApiException;
import com.kimtaeyang.mobidic.repository.DefRepository;
import com.kimtaeyang.mobidic.repository.RateRepository;
import com.kimtaeyang.mobidic.repository.VocabRepository;
import com.kimtaeyang.mobidic.repository.WordRepository;
import com.kimtaeyang.mobidic.type.Difficulty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kimtaeyang.mobidic.code.AuthResponseCode.UNAUTHORIZED;
import static com.kimtaeyang.mobidic.code.GeneralResponseCode.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class WordService {
    private final WordRepository wordRepository;
    private final VocabRepository vocabRepository;
    private final DefRepository defRepository;
    private final RateRepository rateRepository;

    @Transactional
    public AddWordDto.Response addWord(UUID vocabId, AddWordDto.Request request) {
        Vocab vocab = vocabRepository.findById(vocabId)
                        .orElseThrow(() -> new ApiException(NO_VOCAB));
        authorizeVocab(vocab);

        wordRepository.findByExpression(request.getExpression())
                .ifPresent((w) -> { throw new ApiException(DUPLICATED_WORD); });

        Word word = Word.builder()
                .expression(request.getExpression())
                .vocab(vocab)
                .build();
        wordRepository.save(word);

        Rate rate = Rate.builder()
                .word(word)
                .correctCount(0)
                .incorrectCount(0)
                .isLearned(0)
                .build();
        rateRepository.save(rate);

        return AddWordDto.Response.fromEntity(word);
    }

    @Transactional(readOnly = true)
    public List<WordDto> getWordsByVocabId(UUID vId) {
        Vocab vocab = vocabRepository.findById(vId)
                .orElseThrow(() -> new ApiException(NO_VOCAB));
        authorizeVocab(vocab);

        return wordRepository.findByVocab(vocab)
                .stream().map((word) -> {
                    Rate rate = rateRepository.getRateByWordId(word.getId())
                            .orElseThrow(() -> new ApiException(INTERNAL_SERVER_ERROR));

                    Difficulty diff = getDifficulty(rate.getCorrectCount(), rate.getIncorrectCount());

                    return WordDto.fromEntity(word, diff);
                }).collect(Collectors.toList());
    }

    @Transactional
    public AddWordDto.Response updateWord(UUID wordId, AddWordDto.Request request) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new ApiException(NO_WORD));
        authorizeWord(word);

        wordRepository.findByExpression(request.getExpression())
                        .ifPresent((w) -> { throw new ApiException(DUPLICATED_WORD); });

        word.setExpression(request.getExpression());
        wordRepository.save(word);

        return AddWordDto.Response.fromEntity(word);
    }

    @Transactional
    public WordDto deleteWord(UUID wordId) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new ApiException(NO_WORD));
        authorizeWord(word);

        wordRepository.delete(word);

        return WordDto.fromEntity(word, Difficulty.NORMAL);
    }

    @Transactional(readOnly = true)
    public WordDetailDto getWordDetail(UUID wId) {
        Word word = wordRepository.findById(wId)
                .orElseThrow(() -> new ApiException(NO_WORD));
        authorizeWord(word);

        List<Def> definitions = defRepository.findByWord(word);

        return WordDetailDto.fromEntity(word, definitions);
    }

    private Difficulty getDifficulty(Integer correct, Integer incorrect) {
        double diff = calcDifficultyRatio(correct, incorrect);

        if(diff < 0.3){
            return Difficulty.EASY;
        } else if (diff > 0.7) {
            return Difficulty.HARD;
        }

        return Difficulty.NORMAL;
    }

    private double calcDifficultyRatio(Integer correct, Integer incorrect) {
        /*
            난이도 함수 : -0.04correct + 0.05incorrect + 0.5
        */
        double diff = (-0.04 * correct) + (0.05 * incorrect) + 0.5;
        if (diff > 1){
            diff = 1;
        }
        else if (diff < 0){
            diff = 0;
        }

        return diff;
    }

    private void authorizeVocab(Vocab vocab){
        if(!vocab.getMember().getId().equals(getCurrentMemberId())) {
            throw new ApiException(UNAUTHORIZED);
        }
    }

    private void authorizeWord(Word word){
        if(!word.getVocab().getMember().getId().equals(getCurrentMemberId())) {
            throw new ApiException(UNAUTHORIZED);
        }
    }

    private UUID getCurrentMemberId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return ((Member) auth.getPrincipal()).getId();
    }
}
