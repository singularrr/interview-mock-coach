import axios from 'axios'

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000,
})

export async function get<T>(url: string, config?: object): Promise<T> {
  const response = await instance.get<T>(url, config)
  return response.data
}

export async function post<T, D = unknown>(url: string, data?: D, config?: object): Promise<T> {
  const response = await instance.post<T>(url, data, config)
  return response.data
}

export default instance
