import NavButton from './NavButton';
import React from 'react';
import { useLocation } from 'react-router-dom';
import { useNavButtonStyles } from 'materialStyles/navButtonStyles';

const NavButtons: React.FC = () => {
    const location = useLocation();
    const classes = useNavButtonStyles();
    return (
        <div key={location.key} className={classes.navButtonWrapper} style={{ paddingLeft: 6, paddingRight: 6 }}>
            <NavButton label={'Corda Configuration'} path={'/'} icon={<></>} />
            <NavButton label={'Tic-tac-toe'} path={'/tic-tac-toe'} icon={<></>} />
            <NavButton label={'Connect 4'} path={'/connect4'} icon={<></>} />
        </div>
    );
};

export default NavButtons;
