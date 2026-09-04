import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { clearImageCache } from '../api/imageCache'
import * as rooms from '../api/rooms'
import * as storage from '../api/storage'
import RoomsPage from './RoomsPage'

describe('RoomsPage', () => {
  beforeEach(() => {
    clearImageCache()
  })

  it('shows the room image via a blob object URL when roomImage is set', async () => {
    vi.spyOn(rooms, 'listRooms').mockResolvedValue({
      content: [
        {
          id: 1,
          roomId: 'r1',
          roomName: 'Room One',
          roomImage: 'r.jpg',
          memberCount: 2,
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
    })
    const fetchImageObjectUrl = vi.spyOn(storage, 'fetchImageObjectUrl').mockResolvedValue('blob:fake')

    render(
      <MemoryRouter>
        <RoomsPage />
      </MemoryRouter>,
    )

    const img = (await screen.findAllByAltText('Room One')).find((el) => el.tagName === 'IMG') as HTMLImageElement
    expect(img).toBeDefined()
    expect(img.src).toBe('blob:fake')
    expect(fetchImageObjectUrl).toHaveBeenCalledWith('r.jpg')
  })

  it('shows the first letter as a placeholder when roomImage is missing', async () => {
    vi.spyOn(rooms, 'listRooms').mockResolvedValue({
      content: [
        {
          id: 2,
          roomId: 'r2',
          roomName: 'Bob Room',
          memberCount: 1,
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
    })

    render(
      <MemoryRouter>
        <RoomsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Bob Room')).toBeInTheDocument()
    expect(screen.getByText('B')).toBeInTheDocument()
  })
})
