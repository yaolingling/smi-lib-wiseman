package com.sun.ws.management.server;

import java.util.UUID;

public interface ContextListener {
	
	/**
	 * Called to notify the application when a context 
	 * has been successfully created.
	 * 
	 * @param requestContext context of the request
	 * @param context UUID identifying the request
	 */
	 public void contextBound(final HandlerContext requestContext, final UUID context);

		/**
		 * Called to notify the application when a context 
		 * has been released and is no longer valid.
		 * 
		 * @param requestContext context of the request
		 * @param context UUID identifying the request
		 */
	 public void contextUnbound(final HandlerContext requestContext, final UUID context);
}
