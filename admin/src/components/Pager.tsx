interface PagerProps {
  page: number
  totalPages: number
  onChange: (page: number) => void
}

/** page 는 0-based, 표시는 1-based. */
export default function Pager({ page, totalPages, onChange }: PagerProps) {
  const displayTotal = Math.max(totalPages, 1)
  return (
    <div className="pager">
      <button type="button" className="btn btn--secondary btn--sm" onClick={() => onChange(page - 1)} disabled={page <= 0}>
        이전
      </button>
      <span>
        페이지 {page + 1} / {displayTotal}
      </span>
      <button
        type="button"
        className="btn btn--secondary btn--sm"
        onClick={() => onChange(page + 1)}
        disabled={page + 1 >= totalPages}
      >
        다음
      </button>
    </div>
  )
}
