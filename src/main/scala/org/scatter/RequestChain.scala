/*
 * Copyright 2012 @peltchu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.scatter

import cc.spray.http.HttpRequest

private[scatter] class RequestChain(val chain: List[RequestTransformation])
	extends Chainable[RequestTransformation, RequestChain] {

	def build(newChain: List[RequestTransformation]): RequestChain = {
		new RequestChain(newChain)
	}

	def toHttpRequest: HttpRequest = {
		chain.foldLeft(HttpRequest())((currReq, transformer) =>
			transformer.transform(currReq))
	}
}

private[scatter] object RequestChain {
	def apply(transformation: RequestTransformation): RequestChain = {
		new RequestChain(List(transformation))
	}

	def apply(transformations: List[RequestTransformation]): RequestChain = {
		new RequestChain(transformations)
	}
}
