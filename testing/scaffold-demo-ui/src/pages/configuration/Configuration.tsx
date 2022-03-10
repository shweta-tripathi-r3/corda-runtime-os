import ManageCpis from './ManageCpis';
import { Paper } from '@material-ui/core';
import UploadCpi from './UploadCpi';
import VNodeList from './VNodeList';
import useTileStyles from 'materialStyles/tileStyles';

const Configuration = () => {
    const tileClasses = useTileStyles();
    return (
        <div style={{ display: 'flex', gap: 24, paddingLeft: 24 }}>
            <Paper className={tileClasses.tile}>
                <UploadCpi />
            </Paper>

            <Paper className={tileClasses.tile}>
                <ManageCpis />
            </Paper>

            <Paper className={tileClasses.tile}>
                <VNodeList />
            </Paper>
        </div>
    );
};

export default Configuration;
