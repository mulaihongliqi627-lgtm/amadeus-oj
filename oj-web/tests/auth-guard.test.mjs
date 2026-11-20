import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import axios, { AxiosError } from 'axios'
import { createPinia, setActivePinia } from 'pinia'
import { createServer } from 'vite'

const storage = new Map()
globalThis.sessionStorage = {
  getItem: (key) => storage.get(key) ?? null,
  setItem: (key, value) => storage.set(key, value),
  removeItem: (key) => storage.delete(key),
}
let respond
let calls = 0
const originalAdapter = axios.defaults.adapter
axios.defaults.adapter = async (config) => {
  calls++
  return { config, headers: {}, status: 200, statusText: 'OK', data: await respond(config) }
}
const server = await createServer({ server: { middlewareMode: true }, appType: 'custom' })
const { guardRoute, getLoginDestination } = await server.ssrLoadModule('/src/router/guard.ts')
const { useAuthStore } = await server.ssrLoadModule('/src/stores/auth.ts')
const { onUnauthorized, request } = await server.ssrLoadModule('/src/api/client.ts')
after(async () => {
  axios.defaults.adapter = originalAdapter
  await server.close()
})

function setup() {
  setActivePinia(createPinia())
  const auth = useAuthStore()
  auth.clear()
  calls = 0
  respond = () => ({ code: 1000, data: { userAccount: 'admin', nickName: '管理员' } })
  onUnauthorized(() => auth.clear())
  return auth
}
const route = (path, query = {}) => ({
  path,
  name: path === '/login' ? 'login' : path.slice(1),
  meta: path === '/login' ? { public: true } : {},
  query,
})
const token = 'header.payload.signature'

test('未登录直接访问后台：在请求接口和挂载页面前跳转登录', async () => {
  const auth = setup()
  assert.deepEqual(await guardRoute(route('/admins'), auth), {
    name: 'login',
    query: { redirect: '/admins' },
  })
  assert.equal(calls, 0)
  assert.equal(await guardRoute(route('/login'), auth), undefined)
})

test('有本地 token：后台导航先向服务端验证并更新用户信息', async () => {
  const auth = setup()
  auth.signIn(token, 'admin')
  respond = (config) => {
    assert.equal(config.url, '/sysUser/info')
    assert.equal(config.headers.Authorization, `Bearer ${token}`)
    return { code: 1000, data: { userAccount: 'admin', nickName: '已验证' } }
  }
  assert.equal(await guardRoute(route('/dashboard'), auth), undefined)
  assert.equal(auth.displayName, '已验证')
  assert.equal(calls, 1)
})

test('已登录访问 login：验证成功后返回安全的原目标页面', async () => {
  const auth = setup()
  auth.signIn(token, 'admin')
  assert.equal(await guardRoute(route('/login', { redirect: '/admins' }), auth), '/admins')
  for (const value of ['https://evil.test', '//evil.test', '/login', ['/admins'], '/admins?x=1']) {
    assert.equal(getLoginDestination(value), '/dashboard')
  }
})

for (const failure of ['业务码 3001', 'HTTP 401']) {
  test(`${failure}：清除存储并提示失效，只由守卫发起跳转`, async () => {
    const auth = setup()
    auth.signIn(token, 'admin')
    let handlerCalls = 0
    onUnauthorized(() => {
      handlerCalls++
      auth.clear()
    })
    respond = (config) => {
      if (failure === 'HTTP 401') {
        throw new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, null, { status: 401 })
      }
      return { code: 3001, msg: '令牌不能为空' }
    }
    assert.deepEqual(await guardRoute(route('/questions'), auth), {
      name: 'login',
      query: { redirect: '/questions', expired: '1' },
    })
    assert.equal(auth.isLoggedIn, false)
    assert.equal(storage.size, 0)
    assert.equal(handlerCalls, 0)
    assert.equal(await guardRoute(route('/login', { expired: '1' }), auth), undefined)
  })
}

test('网络异常：拒绝进入后台，保留会话，登录页不发生重定向循环', async () => {
  const auth = setup()
  auth.signIn(token, 'admin')
  respond = () => {
    throw new AxiosError('Network Error', 'ERR_NETWORK')
  }
  const destination = await guardRoute(route('/contests'), auth)
  assert.deepEqual(destination, {
    name: 'login',
    query: { redirect: '/contests', unavailable: '1' },
  })
  assert.equal(auth.isLoggedIn, true)
  assert.equal(storage.size, 1)
  assert.equal(await guardRoute(route('/login', destination.query), auth), undefined)
})

test('旧会话校验失败：不清除新登录，不放行未校验的新 token', async () => {
  const auth = setup()
  auth.signIn(token, 'admin')
  respond = () => {
    auth.signIn('new.payload.signature', 'new-admin')
    return { code: 3001 }
  }
  assert.equal(await guardRoute(route('/admins'), auth), false)
  assert.equal(auth.session.account, 'new-admin')
})

test('旧会话校验成功但登录已变化：不覆盖新账号，不放行', async () => {
  const auth = setup()
  auth.signIn(token, 'admin')
  respond = () => {
    auth.signIn('new.payload.signature', 'new-admin')
    return { code: 1000, data: { userAccount: 'old-admin', nickName: '旧账号' } }
  }
  assert.equal(await guardRoute(route('/admins'), auth), false)
  assert.equal(auth.session.account, 'new-admin')
})

test('普通业务请求失效：保留统一处理，并发失效只清理一次', async () => {
  const auth = setup()
  auth.signIn(token, 'admin')
  let handlerCalls = 0
  onUnauthorized(() => {
    handlerCalls++
    auth.clear()
  })
  respond = () => ({ code: 3001, msg: '令牌不能为空' })
  const results = await Promise.allSettled([request({ url: '/test' }), request({ url: '/test' })])
  assert.equal(handlerCalls, 1)
  for (const result of results) {
    assert.equal(result.status, 'rejected')
    assert.equal(result.reason.message, '登录状态已失效，请重新登录。')
  }
})

test('公开的 404 页面无需会话验证', async () => {
  const auth = setup()
  auth.signIn(token, 'admin')
  assert.equal(await guardRoute({ ...route('/missing'), meta: { public: true } }, auth), undefined)
  assert.equal(calls, 0)
})
