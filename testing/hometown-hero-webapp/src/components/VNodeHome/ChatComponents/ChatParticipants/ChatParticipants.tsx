import { Button, Checkbox, TextInput } from '@r3/r3-tooling-design-system/exports';

import SelectedParticipants from '../SelectedParticipants/SelectedParticipants';
import style from './chatParticipants.module.scss';
import useAppDataContext from '@/contexts/appDataContext';
import { useMemo, useState } from 'react';
import useMessagesContext from '@/contexts/messagesContext';
import useUserContext from '@/contexts/userContext';

type Props = {
    selectedParticipants: string[];
    setSelectedParticipants: (participants: string[]) => void;
    handleCloseParticipants?: () => void;
};

const ChatParticipants: React.FC<Props> = ({
    handleCloseParticipants,
    selectedParticipants,
    setSelectedParticipants,
}) => {
    const { vNodes, refreshVNodes } = useAppDataContext();
    const { vNode: myVNode } = useUserContext();
    const { getTotalIncomingMessagesForSender } = useMessagesContext();
    const [nodeFilterQuery, setNodeFilterQuery] = useState<string>('');

    const handleCheckboxClicked = (checkBoxChecked: boolean, participant: string) => {
        if (!checkBoxChecked) {
            setSelectedParticipants(selectedParticipants.filter((p) => p !== participant));
        } else {
            //allows for multiple participant selection
            //setSelectedParticipants([...selectedParticipants, participant]);
            setSelectedParticipants([participant]);
        }
    };

    const networkParticipants = useMemo(
        () =>
            vNodes
                .filter(
                    (node) =>
                        node.holdingIdentity.x500Name !== myVNode?.holdingIdentity.x500Name &&
                        !(nodeFilterQuery !== '' && !node.holdingIdentity.x500Name.includes(nodeFilterQuery))
                )
                .sort((a, b) => {
                    const aMessages = getTotalIncomingMessagesForSender(a.holdingIdentity.x500Name);
                    const bMessages = getTotalIncomingMessagesForSender(b.holdingIdentity.x500Name);

                    if (aMessages > bMessages) return -1;

                    if (aMessages < bMessages) return 1;

                    return 0;
                }),
        [vNodes, myVNode, nodeFilterQuery]
    );

    return (
        <div className={style.chatParticipants}>
            <TextInput
                label="Filter Nodes"
                value={nodeFilterQuery}
                onChange={(event) => setNodeFilterQuery(event.target.value)}
            />
            <SelectedParticipants
                selectedParticipants={selectedParticipants}
                handleClearParticipants={() => {
                    setSelectedParticipants([]);
                }}
            />
            <div className={style.participantsWrapper}>
                {networkParticipants.map((node) => {
                    const selected = selectedParticipants.includes(node.holdingIdentity.x500Name);
                    const x500Name = node.holdingIdentity.x500Name;
                    return (
                        <div className={style.participantContainer} key={x500Name + node.cluster}>
                            <Checkbox
                                checked={selected}
                                value={node.holdingIdentity.x500Name}
                                onChange={(e) => {
                                    handleCheckboxClicked(!selected, x500Name);
                                    e.stopPropagation();
                                }}
                            />
                            <p
                                className={`${selected ? 'text-blue' : ''} cursor-pointer`}
                                onClick={(e) => {
                                    handleCheckboxClicked(!selected, x500Name);
                                    e.stopPropagation();
                                }}
                            >
                                {x500Name}
                            </p>

                            <p className="ml-auto mr-5 text-lg">
                                <strong>{getTotalIncomingMessagesForSender(x500Name)}</strong>
                            </p>
                        </div>
                    );
                })}
            </div>
            {handleCloseParticipants && (
                <Button
                    className={style.confirmParticipants}
                    iconLeft="AccountCheck"
                    size={'large'}
                    variant={'primary'}
                    onClick={handleCloseParticipants}
                >
                    Confirm
                </Button>
            )}
        </div>
    );
};

export default ChatParticipants;
