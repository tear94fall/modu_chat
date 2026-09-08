package com.example.modumessenger.Global;

import com.example.modumessenger.dto.PageResponseDto;

/**
 * 친구 목록 페이지 상태. 화면은 스크롤 끝에서 {@link #canLoad()} 를 보고 {@link #beginLoad()} 로
 * 다음 페이지 번호를 받아 요청하고, 응답이 오면 {@link #onLoaded} / 실패하면 {@link #onFailed} 를 부른다.
 * {@link #reset()} 은 세대(generation)를 올려서, 초기화 전에 보낸 요청의 늦은 응답이 목록을 덮어쓰지 못하게 한다.
 */
public class FriendsPager {

    private final int pageSize;
    private int nextPage;
    private boolean last;
    private boolean loading;
    private long totalElements;
    private int generation;

    public FriendsPager(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageSize() { return pageSize; }
    public boolean isLast() { return last; }
    public boolean isLoading() { return loading; }
    public long getTotalElements() { return totalElements; }
    /** 현재 세대. 요청을 보낼 때 같이 잡아 두었다가 {@link #onLoaded(int, PageResponseDto)} 에 넘긴다. */
    public int getGeneration() { return generation; }

    public boolean canLoad() {
        return !loading && !last;
    }

    /** 요청할 페이지 번호를 돌려주고 읽는 중으로 표시한다. */
    public int beginLoad() {
        loading = true;
        return nextPage;
    }

    public boolean onLoaded(PageResponseDto<?> page) {
        return onLoaded(generation, page);
    }

    /** 응답을 반영한다. 세대가 다르면(초기화 이후 도착한 옛 응답) 무시하고 false 를 돌려준다. */
    public boolean onLoaded(int requestGeneration, PageResponseDto<?> page) {
        if (requestGeneration != generation) {
            return false;
        }
        loading = false;
        last = page.isLast();
        nextPage = page.getPage() + 1;
        totalElements = page.getTotalElements();
        return true;
    }

    public void onFailed() {
        loading = false;
    }

    public void reset() {
        generation++;
        nextPage = 0;
        last = false;
        loading = false;
        totalElements = 0;
    }
}
