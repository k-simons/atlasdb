/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.paxos;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class PaxosLatestRoundVerifierImpl implements PaxosLatestRoundVerifier {
    private static final Logger log = LoggerFactory.getLogger(PaxosLatestRoundVerifierImpl.class);

    private final ImmutableList<PaxosAcceptor> acceptors;
    private final int quorumSize;
    private final ExecutorService executor;

    public PaxosLatestRoundVerifierImpl(List<PaxosAcceptor> acceptors, int quorumSize, ExecutorService executor) {
        this.acceptors = ImmutableList.copyOf(acceptors);
        this.quorumSize = quorumSize;
        this.executor = executor;
    }

    @Override
    public PaxosQuorumStatus isLatestRound(long round) {
        List<PaxosResponse> responses = collectResponses(round);

        return determineQuorumStatus(responses);
    }

    private List<PaxosResponse> collectResponses(long round) {
        return PaxosQuorumChecker.collectQuorumResponses(
                acceptors,
                acceptor -> new PaxosResponseImpl(acceptorAgreesIsLatestRound(acceptor, round)),
                quorumSize,
                executor,
                PaxosQuorumChecker.DEFAULT_REMOTE_REQUESTS_TIMEOUT_IN_SECONDS,
                true);
    }

    private boolean acceptorAgreesIsLatestRound(PaxosAcceptor acceptor, long round) {
        try {
            return round >= acceptor.getLatestSequencePreparedOrAccepted();
        } catch (Exception e) {
            log.info("latest sequence retrieval failed", e);
            throw e;
        }
    }

    private PaxosQuorumStatus determineQuorumStatus(List<PaxosResponse> responses) {
        return PaxosQuorumChecker.getQuorumResult(responses, quorumSize);
    }

}
