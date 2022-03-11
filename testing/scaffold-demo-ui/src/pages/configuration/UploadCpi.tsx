import { Button, Typography } from '@material-ui/core';
import { ChangeEvent, useRef, useState } from 'react';

import { uploadCpi } from 'api/cpi';
import useAppDataContext from 'contexts/AppDataContext';
import { useAppStyles } from 'materialStyles/appStyles';

const UploadCpi = () => {
    const appClasses = useAppStyles();
    const inputRef = useRef<HTMLDivElement>(null);
    const [file, setFile] = useState<File | undefined>(undefined);
    const { refreshCpiList, refreshVNodes } = useAppDataContext();

    const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files.length > 0) {
            setFile(e.target.files[0]);
        }
    };

    const upload = async () => {
        if (!file) return;

        //Might want to store the returned cpi id to poll for its status at some point?
        const cpiUploadResponse = await uploadCpi(file?.name, file);

        refreshCpiList();
        refreshVNodes();
    };

    return (
        <div style={{ width: 350, display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Typography variant="h4" className={appClasses.contrastText} style={{ marginBottom: 16 }}>
                Upload CPI
            </Typography>

            {file && <Typography className={appClasses.secondaryLightText}>{file.name}</Typography>}
            <div style={{ display: 'flex', gap: 12 }}>
                <Button
                    className={appClasses.button}
                    component="label"
                    variant="outlined"
                    color="secondary"
                    onClick={() => {
                        if (!inputRef) return;
                        inputRef.current?.click();
                    }}
                >
                    Choose File
                    <input type="file" hidden onChange={handleInputChange} />
                </Button>
                <Button
                    className={appClasses.button}
                    variant="outlined"
                    color="secondary"
                    onClick={upload}
                    disabled={file === undefined}
                >
                    Upload
                </Button>
            </div>
        </div>
    );
};

export default UploadCpi;
