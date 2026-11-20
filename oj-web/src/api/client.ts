import axios, { type AxiosRequestConfig } from 'axios'
import { readSession } from '../lib/session'

interface ApiResult<T> {
  code: number
  msg: string
  data: T
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly code?: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

let unauthorizedHandler: (() => void) | undefined

export function onUnauthorized(handler: () => void) {
  unauthorizedHandler = handler
}

const client = axios.create({ baseURL: '/api', timeout: 15000 })

export async function request<T>(
  config: AxiosRequestConfig,
  authenticated = true,
  options: { redirectOnUnauthorized?: boolean } = {},
): Promise<T> {
  const token = authenticated ? readSession()?.token : undefined
  const invalidateSession = () => {
    if (options.redirectOnUnauthorized !== false && token && token === readSession()?.token) {
      unauthorizedHandler?.()
    }
  }

  try {
    const response = await client.request<ApiResult<T>>({
      ...config,
      headers: { ...config.headers, ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    })
    const result = response.data
    if (!result || typeof result !== 'object' || typeof result.code !== 'number') {
      throw new ApiError('服务返回了无法识别的响应，请稍后重试。')
    }
    if (result.code === 3001) invalidateSession()
    if (result.code !== 1000) {
      throw new ApiError(
        result.code === 3001 && authenticated
          ? '登录状态已失效，请重新登录。'
          : result.msg || '操作未完成，请稍后重试。',
        result.code,
      )
    }
    return result.data
  } catch (error) {
    if (error instanceof ApiError) throw error
    if (axios.isAxiosError(error)) {
      if (error.response?.status === 401) {
        invalidateSession()
        throw new ApiError(
          authenticated ? '登录状态已失效，请重新登录。' : '登录失败，请检查账号和密码。',
          3001,
        )
      }
      if (error.response?.status === 403) throw new ApiError('当前账号没有执行此操作的权限。', 403)
      if (error.response?.status === 404)
        throw new ApiError('未找到服务接口，请检查后端服务与代理配置。', 404)
      if (error.code === 'ECONNABORTED') throw new ApiError('请求超时，请稍后重试。')
      if (error.response) throw new ApiError('服务暂时不可用，请稍后重试。', error.response.status)
      throw new ApiError('无法连接服务，请检查网络及后端是否已启动。')
    }
    throw new ApiError('操作未完成，请重试。')
  }
}
