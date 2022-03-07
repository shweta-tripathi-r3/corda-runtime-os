import { createContext, useContext } from 'react';

/*
    This can be used to create context which will throw a runtime error if there is no provider for the specificed context
*/
export const createCtx = <A extends {} | null>() => {
    const ctx = createContext<A | undefined>(undefined);
    function useCtx() {
        const c = useContext(ctx);
        if (c === undefined)
            throw new Error(
                `useContext must be called from within a Provider with a value. There may be no provider specified as a parent of the consumer.`
            );
        return c;
    }
    return [useCtx, ctx.Provider] as const;
};
