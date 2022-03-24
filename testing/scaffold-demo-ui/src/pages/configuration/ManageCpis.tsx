import { Cpi } from 'model/cpi';
import CpisList from './CpisList';
import CreateVNode from './CreateVNode';
import { useState } from 'react';

const ManageCpis = () => {
    const [selectedCpi, setSelectedCpi] = useState<Cpi | undefined>(undefined);

    const handleBackButton = () => {
        setSelectedCpi(undefined);
    };

    const selectCpi = (cpi: Cpi) => {
        setSelectedCpi(cpi);
    };

    return (
        <div style={{ width: 350 }}>
            {!selectedCpi && <CpisList selectCpi={selectCpi} />}
            {selectedCpi && <CreateVNode cpi={selectedCpi} handleBackButton={handleBackButton} />}
        </div>
    );
};

export default ManageCpis;
