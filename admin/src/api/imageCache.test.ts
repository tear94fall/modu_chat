import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as storage from './storage'
import { clearImageCache, loadImage } from './imageCache'

describe('imageCache', () => {
  beforeEach(() => {
    clearImageCache()
    vi.restoreAllMocks()
  })

  it('resolves repeated calls for the same filename to one fetch', async () => {
    const fetchImageObjectUrl = vi.spyOn(storage, 'fetchImageObjectUrl').mockResolvedValue('blob:one')

    const [first, second] = await Promise.all([loadImage('a.jpg'), loadImage('a.jpg')])

    expect(first).toBe('blob:one')
    expect(second).toBe('blob:one')
    expect(fetchImageObjectUrl).toHaveBeenCalledTimes(1)
  })

  it('does not cache a rejected fetch, so a later call retries', async () => {
    const fetchImageObjectUrl = vi
      .spyOn(storage, 'fetchImageObjectUrl')
      .mockRejectedValueOnce(new Error('boom'))
      .mockResolvedValueOnce('blob:retry')

    await expect(loadImage('b.jpg')).rejects.toThrow('boom')

    const result = await loadImage('b.jpg')

    expect(result).toBe('blob:retry')
    expect(fetchImageObjectUrl).toHaveBeenCalledTimes(2)
  })
})
