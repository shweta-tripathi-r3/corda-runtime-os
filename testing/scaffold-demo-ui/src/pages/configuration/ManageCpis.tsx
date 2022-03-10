import { Button, TextField, Typography } from '@material-ui/core';
import { ChangeEvent, useState } from 'react';

import { Cpi } from 'model/cpi';
import CpisList from './CpisList';
import { useAppStyles } from 'materialStyles/appStyles';

const ManageCpis = () => {
    const appClasses = useAppStyles();
    const [x500, setX500] = useState<string>('');
    const [selectedCpi, setSelectedCpi] = useState<Cpi | undefined>(undefined);

    const createNode = () => {};

    const handleBackButton = () => {
        setSelectedCpi(undefined);
    };

    const handleNodeNameChange = (e: ChangeEvent<HTMLInputElement>) => {
        setX500(e.target.value);
    };

    const selectCpi = (cpi: Cpi) => {
        setSelectedCpi(cpi);
    };

    return (
        <div style={{ width: 350 }}>
            {!selectedCpi && <CpisList selectCpi={selectCpi} />}
            {selectedCpi && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                    <Typography variant="h4" className={appClasses.contrastText} style={{ marginBottom: 16 }}>
                        Create V Node
                    </Typography>
                    <Typography className={appClasses.contrastText}>V Node x500 Name</Typography>
                    <TextField
                        color="secondary"
                        className={appClasses.textInput}
                        variant="outlined"
                        onChange={handleNodeNameChange}
                        placeholder={'e.g CN=Alice, O=Alice Corp, L=LDN, C=GB'}
                    />
                    <div style={{ display: 'flex', gap: 12 }}>
                        <Button
                            className={appClasses.button}
                            variant="outlined"
                            color="secondary"
                            onClick={handleBackButton}
                        >
                            Back
                        </Button>
                        <Button className={appClasses.button} variant="outlined" color="secondary" onClick={createNode}>
                            Create V Node
                        </Button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ManageCpis;
