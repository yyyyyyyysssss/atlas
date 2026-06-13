import { Flex, Typography } from "antd"
import useFullParams from "../../../../hooks/useFullParams"


const Saml2Callback = () => {

    const { providerName, error, error_description, accessToken, refreshToken } = useFullParams()

    return (
        <Flex
            vertical
        >
            <Typography.Text>{providerName}</Typography.Text>
            <Typography.Text>{accessToken}</Typography.Text>
            <Typography.Text>{refreshToken}</Typography.Text>
            <Typography.Text>{error}</Typography.Text>
            <Typography.Text>{error_description}</Typography.Text>
        </Flex>
    )
}

export default Saml2Callback