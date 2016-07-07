package com.lody.virtual.client.hook.base;

import android.text.TextUtils;

import com.lody.virtual.client.interfaces.IHookObject;
import com.lody.virtual.helper.utils.XLog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lody
 *
 *
 *         对一个要Hook的对象进行包装.
 *
 */
@SuppressWarnings("unchecked")
public class HookObject<T> implements IHookObject<T> {

	private static final String TAG = HookObject.class.getSimpleName();
	private static final boolean sLocalD = false;
	protected T mBaseObject;
	protected T mProxyObject;
	/**
	 * 内部维护的Hook集合
	 */
	private Map<String, Hook> internalHookMapping = new HashMap<String, Hook>();

	public HookObject(T baseObject, Class<?>... proxyInterfaces) {
		this(baseObject == null ? null : baseObject.getClass().getClassLoader(), baseObject, proxyInterfaces);
	}

	public HookObject(ClassLoader cl, T baseObject, Class<?>... proxyInterfaces) {
		this.mBaseObject = baseObject;
		if (mBaseObject != null) {
			if (proxyInterfaces == null) {
				proxyInterfaces = baseObject.getClass().getInterfaces();
			}
			mProxyObject = (T) Proxy.newProxyInstance(cl, proxyInterfaces, new HookHandler());
		}
	}

	public HookObject(T baseObject) {
		this(baseObject, (Class<?>[]) null);
	}

	/**
	 * 添加一个Hook
	 * 
	 * @param hook
	 *            要添加的Hook
	 */
	@Override
	public void addHook(Hook hook) {
		if (hook != null && !TextUtils.isEmpty(hook.getName())) {
			if (internalHookMapping.containsKey(hook.getName())) {
				XLog.w(TAG, "Hook(%s) from class(%s) have been added, can't add again.", hook.getName(),
						hook.getClass().getName());
			}
			internalHookMapping.put(hook.getName(), hook);
		}
	}

	/**
	 * 移除一个Hook
	 * 
	 * @param hookName
	 *            要移除的Hook名
	 * @return 移除的Hook
	 */
	@Override
	public Hook removeHook(String hookName) {
		return internalHookMapping.remove(hookName);
	}

	/**
	 * 移除一个Hook
	 * 
	 * @param hook
	 *            要移除的Hook
	 */
	@Override
	public void removeHook(Hook hook) {
		if (hook != null) {
			removeHook(hook.getName());
		}
	}

	/**
	 * 移除全部Hook
	 */
	@Override
	public void removeAllHook() {
		internalHookMapping.clear();
	}

	/**
	 * 取得指定名称的Hook
	 *
	 * @param name
	 *            Hook名
	 * @param <H>
	 *            Hook类型
	 * @return 指定名称的Hook
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <H extends Hook> H getHook(String name) {
		return (H) internalHookMapping.get(name);
	}

	/**
	 * @return 包装后的代理对象
	 */
	@Override
	public T getProxyObject() {
		return mProxyObject;
	}

	/**
	 * @return 原对象
	 */
	@Override
	public T getBaseObject() {
		return mBaseObject;
	}

	/**
	 * @return Hook数量
	 */
	@Override
	public int getHookCount() {
		return internalHookMapping.size();
	}

	private class HookHandler implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Hook hook = getHook(method.getName());
			if (hook != null && hook.isEnable()) {
				if (sLocalD) {
					XLog.i(TAG, "Call(hooked) %s", method.getName());
				}
				return hook.onHook(mBaseObject, method, args);
			}
			if (sLocalD) {
				XLog.i(TAG, "Call(unhook) %s", method.getName());
			}
			Object result = method.invoke(mBaseObject, args);
			if (result == null && sLocalD) {
				XLog.d(TAG, "%s return a null value.", method.getName());
			}
			XLog.d(TAG, "call %s(%s) -> %s", method.getName(), Arrays.toString(args), result);
			return result;
		}
	}

}
