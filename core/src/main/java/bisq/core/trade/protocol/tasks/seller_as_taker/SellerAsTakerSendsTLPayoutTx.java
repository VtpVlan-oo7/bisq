/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.trade.protocol.tasks.seller_as_taker;

import bisq.core.trade.Trade;
import bisq.core.trade.messages.SignTLPayoutTxMessage;
import bisq.core.trade.protocol.tasks.TradeTask;

import bisq.network.p2p.NodeAddress;
import bisq.network.p2p.SendMailboxMessageListener;

import bisq.common.taskrunner.TaskRunner;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

//TODO implement
@Slf4j
public class SellerAsTakerSendsTLPayoutTx extends TradeTask {
    @SuppressWarnings({"WeakerAccess", "unused"})
    public SellerAsTakerSendsTLPayoutTx(TaskRunner taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();
            final String id = processModel.getOfferId();
            SignTLPayoutTxMessage message = new SignTLPayoutTxMessage(processModel.getOfferId(),
                    processModel.getMyNodeAddress(),
                    UUID.randomUUID().toString());
            trade.setState(Trade.State.TAKER_SENT_DEPOSIT_TX_PUBLISHED_MSG);

            NodeAddress peersNodeAddress = trade.getTradingPeerNodeAddress();
            log.info("Send {} to peer {}. tradeId={}, uid={}",
                    message.getClass().getSimpleName(), peersNodeAddress, message.getTradeId(), message.getUid());
            processModel.getP2PService().sendEncryptedMailboxMessage(
                    peersNodeAddress,
                    processModel.getTradingPeer().getPubKeyRing(),
                    message,
                    new SendMailboxMessageListener() {
                        @Override
                        public void onArrived() {
                            log.info("{} arrived at peer {}. tradeId={}, uid={}",
                                    message.getClass().getSimpleName(), peersNodeAddress, message.getTradeId(), message.getUid());

                            //TODO set correct (new) state
                            // trade.setState(Trade.State.TAKER_SAW_ARRIVED_DEPOSIT_TX_PUBLISHED_MSG);
                            complete();
                        }

                        @Override
                        public void onStoredInMailbox() {
                            log.info("{} stored in mailbox for peer {}. tradeId={}, uid={}",
                                    message.getClass().getSimpleName(), peersNodeAddress, message.getTradeId(), message.getUid());
                            //TODO set correct (new) state
                            //trade.setState(Trade.State.TAKER_STORED_IN_MAILBOX_DEPOSIT_TX_PUBLISHED_MSG);
                            complete();
                        }

                        @Override
                        public void onFault(String errorMessage) {
                            log.error("{} failed: Peer {}. tradeId={}, uid={}, errorMessage={}",
                                    message.getClass().getSimpleName(), peersNodeAddress, message.getTradeId(), message.getUid(), errorMessage);
                            //TODO set correct (new) state
                            // trade.setState(Trade.State.TAKER_SEND_FAILED_DEPOSIT_TX_PUBLISHED_MSG);
                            appendToErrorMessage("Sending message failed: message=" + message + "\nerrorMessage=" + errorMessage);
                            failed();
                        }
                    }
            );
        } catch (Throwable t) {
            failed(t);
        }
    }
}
