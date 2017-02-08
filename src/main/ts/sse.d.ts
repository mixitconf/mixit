// Type definitions for Server-Sent Events
// Specification: http://dev.w3.org/html5/eventsource/
// Definitions by: Yannik Hampe <https://github.com/yankee42>

declare var EventSource : sse.IEventSourceStatic;

declare module sse {

    /** The readyState attribute represents the state of the connection. */
    enum ReadyState {

        /** The connection has not yet been established, or it was closed and the user agent is reconnecting. */
        CONNECTING = 0,

        /** The user agent has an open connection and is dispatching events as it receives them. */
        OPEN = 1,

        /** The connection is not open, and the user agent is not trying to reconnect. Either there was a fatal error or the close() method was invoked. */
        CLOSED = 2
    }

    interface IEventSourceStatic {
        new (url: string, eventSourceInitDict?: IEventSourceInit): IEventSourceStatic;
        /** The serialisation of this EventSource object's url. */
        url: string;
        withCredentials: boolean;
        /** Always 0 */
        CONNECTING: ReadyState;
        /** Always 1 */
        OPEN: ReadyState;
        /** Always 2 */
        CLOSED: ReadyState;
        /** The ready state of the underlying connection. */
        readyState: ReadyState;
        onopen: (event: Event) => any;
        onmessage: (event: IOnMessageEvent) => void;
        onerror: (event: Event) => any;
        /** The close() method must abort any instances of the fetch algorithm started for this EventSource object, and must set the readyState attribute to CLOSED. */
        close: () => void;
        addEventListener: (type: string, h: (event: IOnMessageEvent) => void) => void;
        removeEventListener: (type: string, h: (event: IOnMessageEvent) => void) => void;
    }

    interface IEventSourceInit {
        /** Defines if request should set corsAttributeState to true.  */
        withCredentials?: boolean;
    }

    interface IOnMessageEvent {
        data: string;
    }
}