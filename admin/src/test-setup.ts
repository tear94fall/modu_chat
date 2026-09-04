import '@testing-library/jest-dom/vitest'

// jsdom 은 createObjectURL/revokeObjectURL 을 구현하지 않는다. RemoteImage 가 쓴다.
if (!URL.createObjectURL) {
  URL.createObjectURL = () => 'blob:stub'
}
if (!URL.revokeObjectURL) {
  URL.revokeObjectURL = () => {}
}
