/*******************************************************************************
 *    Copyright 2016 University of Mannheim
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *******************************************************************************/
package de.unima.core.storage;

import java.util.Optional;

/**
 * Abstraction over connections provided by different triple stores.
 */
public interface StoreConnection {

	/**
	 * Does this connection allow transactions?
	 * 
	 * @return true if transactions are allowed; false otherwise
	 */
	default boolean areTransactionsSupported(){
		return false;
	}
	
	/**
	 * Begins transaction.
	 * 
	 * By default an {@link UnsupportedOperationException} is thrown.
	 * @param lock type of data access
	 * @return token identifying a transaction
	 * @throws UnsupportedOperationException
	 *             if this connection does not support transactions
	 * @throws IllegalStateException
	 *             if transaction creation is not possible
	 */
	default long beginTransaction(Lock lock) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Commits transaction identified by given token.
	 * 
	 * By default an {@link UnsupportedOperationException} is thrown.
	 * 
	 * @param transactionToken
	 *            identifying the transaction which should be committed.
	 * @throws UnsupportedOperationException
	 *             if this connection does not support transactions
	 * @throws IllegalStateException
	 *             if this connection cannot be committed
	 */
	default void commitTransaction(long transactionToken) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Rolls transaction identified by given token back.
	 * 
	 * By default an {@link UnsupportedOperationException} is thrown.
	 * 
	 * @param transactionToken
	 *            identifying the transaction which should be aborted
	 * @throws UnsupportedOperationException
	 *             if this connection does not support transactions
	 * @throws IllegalStateException
	 *             if this connection cannot be aborted
	 */
	default void rollbackTransaction(long transactionToken) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Tries to cast wrapped connection to the given target type.
	 * 
	 * In this way the store specific connection can be retrieved and directly
	 * manipulated.
	 * 
	 * @param targetType
	 *            target type
	 * @param <T> target type
	 * @return instance as target type or empty if not possible
	 */
	default <T> Optional<T> as(Class<T> targetType) {
		final Object wrappedStoreConnection = getWrappedStoreConnection();
		if (targetType.isInstance(wrappedStoreConnection)) {
			return Optional.of(targetType.cast(wrappedStoreConnection));
		}
		return Optional.empty();
	}
	
	/**
	 * Returns the storage specific storage connection.
	 * 
	 * By default this method returns the instance of this store connection. Any
	 * sub-type should override this method.
	 * 
	 * @return instance of the wrapped store connection
	 * @see #as(Class) as typesafe retrieval alternative
	 */
	default Object getWrappedStoreConnection(){
		return this;
	}

	final class Identity implements StoreConnection {
	}
}
