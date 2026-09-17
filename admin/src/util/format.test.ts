import { describe, expect, it } from 'vitest'
import { formatPrice } from './format'

describe('formatPrice', () => {
  it('groups thousands and appends 원', () => {
    expect(formatPrice(89000)).toBe('89,000원')
    expect(formatPrice(0)).toBe('0원')
  })
})
