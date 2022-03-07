import { Theme, makeStyles } from '@material-ui/core';

import { PollRequest } from 'model/common';
import React from 'react';

const useStyles = makeStyles((theme: Theme) => ({
    table: {
        width: '100%',
        height: 'fit-content',
        color: 'white',
        borderCollapse: 'collapse',
        '& table, td, th': { border: '1px solid white' },
        '& td, th': {
            padding: '5px 8px 5px 8px',
        },
        '& td': {
            maxHeight: 20,
        },
    },
}));

type Props = {
    requestIds: PollRequest[];
};

const RequestTable: React.FC<Props> = ({ requestIds }) => {
    const classes = useStyles();

    return (
        <table className={classes.table}>
            <thead>
                <tr>
                    <th>Request Id</th>
                    <th>Status</th>
                    <th>Flow Id</th>
                    <th>Flow Result</th>
                </tr>
            </thead>
            <tbody>
                {requestIds.map((req) => {
                    return (
                        <tr>
                            <td>{req.requestId}</td>
                            <td style={{ color: req.flowStatus.flowStatus === 'COMPLETED' ? 'lightgreen' : 'yellow' }}>
                                {req.flowStatus.flowStatus}
                            </td>
                            <td>{req.flowStatus.flowId}</td>
                            <td>{req.flowStatus.flowResult}</td>
                        </tr>
                    );
                })}
            </tbody>
        </table>
    );
};

export default RequestTable;
