package com.example.modumessenger.Global;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.modumessenger.dto.PageResponseDto;

import org.junit.Test;

import java.util.Collections;

public class FriendsPagerTest {

    private PageResponseDto<String> page(int page, boolean last, long total) {
        return new PageResponseDto<>(Collections.emptyList(), page, 50, total, last ? page + 1 : page + 2, last);
    }

    @Test
    public void 처음에는_0페이지부터_읽을_수_있다() {
        FriendsPager pager = new FriendsPager(50);

        assertTrue(pager.canLoad());
        assertEquals(0, pager.beginLoad());
        assertEquals(50, pager.getPageSize());
    }

    @Test
    public void 읽는_중에는_다시_읽지_않는다() {
        FriendsPager pager = new FriendsPager(50);
        pager.beginLoad();

        assertFalse(pager.canLoad());
    }

    @Test
    public void 한_페이지를_받으면_다음_페이지로_넘어간다() {
        FriendsPager pager = new FriendsPager(50);
        pager.beginLoad();
        pager.onLoaded(page(0, false, 120));

        assertTrue(pager.canLoad());
        assertEquals(1, pager.beginLoad());
        assertEquals(120, pager.getTotalElements());
    }

    @Test
    public void 마지막_페이지를_받으면_더_읽지_않는다() {
        FriendsPager pager = new FriendsPager(50);
        pager.beginLoad();
        pager.onLoaded(page(2, true, 120));

        assertFalse(pager.canLoad());
        assertTrue(pager.isLast());
    }

    @Test
    public void 실패하면_같은_페이지를_다시_읽을_수_있다() {
        FriendsPager pager = new FriendsPager(50);
        assertEquals(0, pager.beginLoad());
        pager.onFailed();

        assertTrue(pager.canLoad());
        assertEquals(0, pager.beginLoad());
    }

    @Test
    public void 초기화하면_처음부터_다시_읽는다() {
        FriendsPager pager = new FriendsPager(50);
        pager.beginLoad();
        pager.onLoaded(page(0, true, 3));
        pager.reset();

        assertTrue(pager.canLoad());
        assertEquals(0, pager.beginLoad());
        assertFalse(pager.isLast());
        assertEquals(0, pager.getTotalElements());
    }

    @Test
    public void 늦게_도착한_이전_요청_응답은_무시한다() {
        FriendsPager pager = new FriendsPager(50);
        pager.beginLoad();
        int staleGeneration = pager.getGeneration();
        pager.reset();
        pager.beginLoad();
        int freshGeneration = pager.getGeneration();

        assertFalse(pager.onLoaded(staleGeneration, page(0, true, 1)));
        assertFalse(pager.canLoad()); // 아직 새 요청이 진행 중
        assertTrue(pager.onLoaded(freshGeneration, page(0, false, 60)));
        assertEquals(1, pager.beginLoad());
    }
}
