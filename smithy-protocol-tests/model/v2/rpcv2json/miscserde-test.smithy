$version: "2.0"

namespace smithy.protocoltests.corpus

use smithy.protocols#rpcv2Json
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply EndpointHostPrefix @httpRequestTests([
    {
        id: "RpcV2JsonEndpointHostPrefix"
        documentation: """
            Operations prepend a static prefix to the endpoint host when they
            carry the @endpoint trait. Only the host is affected: the request
            path still addresses the service and operation as usual."""
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/EndpointHostPrefix"
        body: "{}"
        bodyMediaType: "application/json"
        host: "example.com"
        resolvedHost: "data.example.com"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
    }
])

apply EndpointHostLabel @httpRequestTests([
    {
        id: "RpcV2JsonEndpointHostLabel"
        documentation: """
            The @hostLabel member is substituted into the @endpoint hostPrefix
            AND still serialized into the request body — binding a member to
            the host does not remove it from the payload."""
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/EndpointHostLabel"
        body: """
            {
                "label": "bar"
            }"""
        bodyMediaType: "application/json"
        host: "example.com"
        resolvedHost: "data.bar.example.com"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: { label: "bar" }
    }
])

apply IdempotencyTokenOp @httpRequestTests([
    {
        id: "RpcV2JsonIdempotencyTokenAutoFill"
        documentation: "Automatically populates an idempotency token that was not set"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/IdempotencyTokenOp"
        body: """
            {
                "token": "00000000-0000-4000-8000-000000000000"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        appliesTo: "client"
    }
    {
        id: "RpcV2JsonIdempotencyTokenProvided"
        documentation: "Uses an explicitly provided idempotency token as-is rather than generating one"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/IdempotencyTokenOp"
        body: """
            {
                "token": "8a3e2f1c-5b6d-4e7f-8091-a2b3c4d5e6f7"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: { token: "8a3e2f1c-5b6d-4e7f-8091-a2b3c4d5e6f7" }
    }
])

apply RequestCompressionOp @httpRequestTests([
    {
        id: "RpcV2JsonRequestCompressionGzip"
        documentation: "Compression algorithm encoding is appended to the Content-Encoding header"
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/RequestCompressionOp"
        headers: {
            "smithy-protocol": "rpc-v2-json"
            "Content-Type": "application/json"
            Accept: "application/json"
            "Content-Encoding": "gzip"
        }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: {
            data: """
                DET5w2onta019lTPCeLsrdlWLLmMCSRtJydsjLdmiurEenOVtdamJoWVIS5HiAuq4ly8DsLX45dgEXco
                UXMV165WXbF1RohhGbYWMjXRaQoW9SLfcvXvf6G5mzWLKeaywnQ65KWTcdD04xy6oQS2gO4zun1GHZJf
                2l6seEAYbD6kfDO0I6y4Hhz81qYtCzWe99dquiSrqyw3HSOZR03X3d0aA4gF7ENnlPbwxT3phKQJtfU5
                WKVllatawTmxoIBF59Ge9PTXUIky6EJ7wRTouwdHSiYns38frHOBIzmMrMcxXK7kQn03rVf1JvXbgjsC
                rpTcld0hGv1o1z4sSECUC0DrIr9h3GCkxEQqfQdd7oS85vVzumxHEmFANrQfwruhL9ooFX5N1hEmeggS
                kkdnWi69rhrzfY5sCvzzUt3f579B7CBAXhvHXFhJejbQnZ6a6xmWTFKBHJ5ZcETULMXzoa7ngoi0F9Re
                hirSRSxPXdpXPH67oxaHcaJWSG7nKoF6nfZh1xwWbr7dI1GWiKpcyhuZky2nXRxzSGsC1ycK16YfwEGM
                RKPCCKQpFWIUrZn3bDvKWrZfo2cb8JWYDS9Sscz7oUFuboBltY8jZP7Lahw0TmCZ5LzRb8Sc4Tx81wQD
                OdPoe5WG9gM74NfReTLIjSyzn6KEsIwMXYaUwPGSBL4jJc8oegzQwS5UphGBtD4XX0Mtl35w7OtlTQGh
                8iwsVvbqav0mI8GHsFEPUpOerk59hYL0rBC0VLL6SWNRRE5zAoq2aA8Hh0yR1AqeasR1wU5snKyVjsGz
                4ftR1WsKpVqNfLKBLXHveIMbycXuTcLLxA1CWD4LXzLyp3XODOhJrs3rTE1KA1F4tHc0UG36MnykEu3E
                Bnwa1D2SntbK9XGKZ86JYX7a8GkC3ZiB309IoMd4HnWrKU02ssK6sYEvMPXX3ytEwRWjMRnSDjllYPY1
                R4EUwnewzKOnnvMS9JtfSk6kxcai2snF3Lysnt2SdXORDMV0Wmy2wsjZt9FqgTgX5jv0F5eOGf9qrvUS
                qAeHmT6L1wsDDkXmSnB2EwsszxxkastVjw8h6EfrGPQyIgjoJffa9NRbvKxUlG9qI7WMF2nVj68vhuWH
                XshfIfH12RHxgwBI4o23yfHxDDw5yDmLrgkEhkKk8yObZaatLx7MOJ2YoDnpfRMiakZ6ZtB0ifFqJbPh
                cQiORHpKQTjrdaY8sHfUdoAh0nuVPTzDXokw7DYrN9hraq0TykJe74mLcVjr61XBuuTf9SOmiG52tiCF
                GohK9wmYNdWR3YgcQ9EUQEsfFcnMEhBgDXeXDfocZgsI3XucIRFt4C4UNCWVUyJvWMzJGhiFlYcV6bmB
                5tX5jTn2sDnysG5hBbOgdUk1pbAY5nBg4f03mM6WRBwT4ahr80hy8fmoQVyn9e2KZpPQxvVSg4yHg75J
                OgrpC95AtKJftph6xlCFZxsJUBHTWISfQFZKt5DqpVhgGO2Iq5IGNsaw4qLL3rVHC3edtmpFtkMJjUTc
                juGK99fBiHs6dXAhgzlquSrk3BiAStHuN1FWNjMV15YdiHnCRexJ7j4W2XTgit5Syp2Vq4uhl93fkL5i
                33IQT9BiqnhwYsDBZjh7q8cckZrETQ9CU1NOw8sHw3jOtTjRtIEZuPl5FnYBfbr7ROXQpcix7Hp0Q5f9
                3YlgipkDi8t43ie8le1ZrVt9isKx5bcV4urondjdqhAj367HahTXewSkbvS32aQYs4ZO62te2COf6bol
                lJ4u5N1oHu1te3Sx9XIjEK9xeWdpyQnACM6ClszT5L0zSD46iLFaTPggUT1emksOz7eisjVSxEtrZWn3
                kFcpcxJU0PNK2XIGjaBZCWb9ewEX51B5GrA2LXta3IXm1EUOIaXEg1fTSrFfGRTxjEwQ5odkiLTesdtf
                TFZ82A3Rt4MpYxkeeizMoKkS49sQWSuqQOr2qD0RHQV7jSZN7mVI4GCneD5dYHyWSxkFhkgaf7ssO7Er
                JCcE7tt55myDxXAqaK33PhrgrCY7u5zMOzzuvktCIxIi4HEyDbLYk3npw27YzWc2CbK8zzl8CEAxcTnA
                2daD1809BhzPXNHeKbYG2LwqMBQK2HJX2buC2Tfangk5C2IHiX1AhtXsj3OHkbushGacJRkxwzsrTRV7
                Vq6PaYvdFgfwnuj85EaW1176UBP2HzNHpvjVE1gn55U095VQLCn3orbG1pyCKmI9KsIp5WX5xzDjLD0q
                mCk8E1YuIZ1pFd5DMSpSj1UjvB62nPg0MxkAuQ3IcHiKk7ho7xYaOQ4DyBUZtMtvAMiXpzCHO6hrj0NL
                opUg6lDVCI4Tvzx3hWKf068NqP6ftVpmRBXGfLbicUqYPIDhJF0XuNZrRH2DT1JH31ZPBnUjW43nRiW6
                CToHZEoDygAFJoVwRCvL5xaKbsXKsXIvNYY7G7RbVMKHpU2m8mOY4CP1QC3ty2tYuWnEX7jXiqN4suBb
                RQbBpJ2VeTWpBYZBRfrcASCThdrB1bd9ewl2AgYyiVsTz1VZuzACvtbHDyrmnYqHvcoq4QLfbdUGAItH
                gJXF2smHcmDDpqBSASBuaj7kjqkBZAtVBCDQKJzp0PAxTc9GspScO6Yk7ZoFspQoRmG3cmGk8OFJFeW6
                XhvQpnmhAOHV4iMPxhL5d5dKDgDs5R5Yawvd8yhi7Y0bK3o8jFMiifnfTlkxXCH7dCEttCO5pMtGffTX
                DCC371LN9Zd01yo7k7gIwk7iNrPcKWT1QgWXypKYT3q7DlYE2pjmw0VP5ZvTPkLlgQjlZXM9U61x3kFo
                Z3nQkORVTmgD2sIIKuVnYLlJejZaRCOG42jPAtKgnRdBWCjnvHnlJuToQjoudjkMAR2Os9R3j0Eek9dw
                WOsc5XfSJxRbRxQD4zKtD3RmnFd4RHXMA0ZeU6Ixjfc905q9NXkFQ2LKF0B2jK5AlIpN3Vcq1F6XELoH
                A692VBU2KMJ7lSm4G6DwNxQ6FdP61AF16Y1obgUZN2TDQg2BNqeqKNAR8fXZNjm0qk7QCUFXtaQgJFcg
                k5KVJJaYD3MlhyN0R16kXJj3C9Ka0b6VY5ffPtYCEo0HyRcBnGwu3Zzns1WKOA1fbeow73PymWlIFZbA
                n9fhtV7gqA1UgW1tJj3USQbRnq9LtqoRzXtnccztjeguy3ZTh2PAO1pOZo3a8IlLIIHWYMJr6384ANZi
                7eJhxRlkbxLz5Nd9BRrrJuKQFYTx0sAHDUsPBdC1ZpExpcnzbB3izlyB2huWFD2Sf990tFfn6A72vZXb
                e9YxhDuWsWMjt8PQ69jdF49lUE4Z9l7oCTEwKstTc65M9jxx0ViyXT4bx8fWil8mMMwMW5PZW1evIXiO
                jcvL4oETa3LJjuJR6USyLWFkXM0tIKj11Q0tjFiiK1TLswNd0RYSXy3Ptl91br4XWYj3j8KterOxw7P6
                g3Um2D5GeOw9De3YJv8Tj5wN22wNWGff5cUJuE9j8pZiKpLj6YFZXHTitiM9ESrc5Mf2DMzmNsubyUck
                UByTtjzhRGxz4xEE4ZajTDA2q8445Ts2bDUPqr3r9Si00vcIBUmg5dUDeS2dNZkvjBVFsyQnuD2krROh
                sXMH11XQmCdOF5wY0vTSr4c8LfTRLKO9TfjVsVL2OeJxrTGDG7tCpj3JJvBwQjLAu23OTY9Eah7nBFmU
                exF5aCQsiYZnjG5VXAkFLhMPyUyPbkqjEDTwiNtlWzNXvNKeFTwNTLhgwWqnLsl7q5Q2QmIPdFIe7LrR
                1PkD7zQdgNdmEUgOn0MRJMIvQuz24vf5y1xomiMmOigNXpGT8ffbqMKopYaRkySlmbK83ZjM5jHeuCkk
                gXABE4yofg10R4pHrHOMMHSvOA6tHheZ9nQVMm9B45rllpwGXlCojpN7gHL4Ea5o2gtkOtMxBTw7wGrm
                8kw1qoKlKeUl98u0Wpci6hc9T8dZQQ3X3WFMcxN5Bu1mUlBTWBeUIvT22c9owPcQAm2SI6mLo6it00uf
                Ul2hjCLoMfPs3JFTdg3ah3Xg9HJoaJuXuiVCIwaaHgPWJxkVpemKVTzzT7ZmW4ONQbFic51EkPq6qQ3h
                86zh5R3NyAnXSDNOiUThCevDNwbMlCRqK17AKLcAj0rAfsfVypHnkd9FKfTi1UqW4iat4rvDxdaXRvpa
                OKjGOStCfZB0H0sNZsb6wILrdkbkQPfNQrI7SjL6f129RN4SUjU3Guumsxvdzug72RFQ8eT6M2vWWBso
                xWEJlGWJzPGgaYpkJyAag9woYaWHi68hNAPlgJhzrFPLBjoEBziTpqtFCWbxGgPsMe27XOqMz47WPaOH
                seF2cJ6qwJXkA6xzJaEIl3w2u111RvU23hfdP2zDSI3ARrS4gpJtlKFPEivurZwKNG10y04OQlkkzFkX
                6rnWODYCfo1K0Rv6IACwJzGbZfP2c4WCH7E4oLGjuuTCjG4C87sACTHyGuzL1zpo69dnf17CkEPRkAFY
                ZSxyXjJtuBt9ssqLsYOvcAAXce2Rz5Y5Ln4EDZ6xsDXBGeBpcLos8YvLLJoOenPjtLQcZWlkkv44jLQm
                lMv46cfM12jrUrhhwTQMQ1lUm4JivnMU3CmnXQzUEjDpglZwGvWAqKvflHrAhC3Z8OqfW7YPiuveQcJe
                d1k8xJJ4DPI1VqcvLjnXvbfEnQI8V8VLXSEg9bbvRMsIdJ2wekCgKBuGt8c44jAuJ9u68buugWOxx6HI
                boeLcQaMHmsKRBLB6i8yVEGhgJWIeO3uwUnu6VBfGGeOmk0g5xgAIL7MZEHuRbIOL6HhJ6ot1MoL04yh
                haS7Qn9uq83npjkY5SHhLhfSfHZOewI53CgJ0dlNE0yUT2cfXOq1FjMLeIogaQgSI1J6OAeJYko29bsf
                NMW4uUvckyWksLts6brGkimFAaKQvmcmqyYyGvuC9hMZIxr78g7dpgtOgZwHZBbykog83jVjPOYoOS2m
                evcbEV1LwDTPOTJPVlEIMjQFKe8hbNaQVdoAegkRFOBCRWTIXFtcR6RoeeCeDE1oRxlRdHn0BxUgpKzG
                GcaSg7saU2IgqAKErONvtaYz0WKY7UBocggt0DBBEsfF7qEFvzRjCqVO7TYlMozi8aa6ftwyT51OU5AO
                3oy8b1ECqpLrF4siIyfsZuKbauZb7h7q639nMOZrYQIaxcJbJTsIjA2ZD7J46B9Fw0iXuYRxuf0TtKhK
                kgNz6BuAgHslFnEG9qAVIU0Ndd2GhcdHFs9XIj4XIEVjMfwSw5RNkJBhYpRxjW6DU63phbGbHECMa2JR
                VWWh3FRzEpM52ING3MJBo0ADQfcMCzSIjOwPtmyBcgIhLOFy5EE1rQZVTuJWBPA5sEdXZ7wQijHfSCCI
                0laD6U7Hl0oWPPzC4B0oMmAGXJuMoSSxa4VBhbY73akwAq0Z4o1tn5cKCzzwgD3X2VhpucN8r6CZ62gH
                av9Yh5VK3BwTjBrr3geLzZRdNEK78qpTw5Re0SFr6dcpT19NcfUZuYgb6vuFTyZqPcywYo5h7xFdjh5l
                WYdGcezH08usIwM4gQmFLPZXai33ttDT681bkZSRjeG7lHas34SoSRf8CoOmdzOtkmuucx1z5t2M9wUX
                eBP5tPhJqdLz5RAdLQLjZJwiVFs7UDZv9zGFMeDpRtjO0CDmv1b0kCEDPFAzkTKErY2beFC1tPEfmmi3
                kHyxpCfvi1NqjPaXJkVmV7ySY3PkXqs9sicy0Fi36c9LCVhJnmgEFslqPgChNfFOayvMs4O1iJHEHu6l
                PZj0ydjWIJmY3oTMR2JtEAHUWNzTtzZTcl22hW7zvIibITTDkAN5ayszwWIDFYpheHuupLU3Ub45PhWc
                wy4RTEzOFLcZ9tcDuJyfTFbkFHDcZ6Ev57LzQhtIje1uZ0ojZnVttDGqWlPhb1oEhx6yblnZwm5fCBz0
                LDInqIibhHJkOoxd12kKCuvLF3529ur1JAdUCIEtLqRWEG4zf9tg4yCkLLLO0zr8LBeXqg8Fo5NvdAMM
                erlP4Mw4iveQeZJmE0oYc5YDnRBFzWtIALGNY8PNUZikogqVDl6ep5hQS97Ucg3GDblOQub7hR4FxUC2
                tp1hb1QK0L6qigWcrqoWphoeFxfdG0w2eVb6Hx8EocCmOo27IvKA1yfeiGdn01olIJbNu85wKFBFCX6U
                tDHJb3M2oRlDOpXKCbAllELrtyS8LyNWIL40lWOByI7yp1M3HMDlZ0H5JWJHQGPaVNG2uHHf4afSse9R
                qF3UfDfMxecRF8AhB3cwplqVx71egH6TmyHz2lQkyeSeP1NV0s4A6rDadNjeXiMiMhDgWORe0Hs19ZZy
                HEfmCsq4CZb9TEpaHn6itMWp45JigMrWaSdlvZ3JsRT4NUQNySCTv43IrCz7kkdSfuVkyZ6sKGzUSvpO
                DBY4tC9RBTVhZ53dJlsmjNKn9VwrMJNdOl6A6Xn2FUnIS38PzRvF4Pdm2C7KCedK6ZYReZZCEFumKSN2
                sAnY3USiy8kkrgiSuXY27n0qh7HByNwtNPSWKyxbjQJ5EGFBBao3qLGgbO21IkRCR4nsml1wYB9zd07L
                Cz4z6BZ08olg92PGBigAZHeyZIwZOVLVv2p3dmq3Ypp9St8uVJRn0cBRHfo5tW8BXLuWyLCBAvmmeKQ6
                ZJQ1APnyfUPnupo1Jf2I3cpSm7QgkfCB77UN4DlTWu6xowgJ8I7qJAKCbgib7lkEwAjwJ28BiWTaZzdQ
                mK4UR6vRZ194aVa4VeWKQVfaLd9reqnqgvZQTRrhGg5GbIMPRewE4vTugMv6UHzK7gmyTjvoZ0OcTU3u
                2gnzRkbIeOOM14IU1IsaYsm1NinOQgWKmqm1dct9Rfge2OfCifcR7LD98EzsXCELz7hpSqeXqjtP6KLt
                OTEqpW23EsDRJmKBqzKMMrsbQmG4XyasVDry7a3zGp0IULsmZguJQ7tw3VrQp9XJSYOtpQtLgSUANx0D
                MfzQOGyVt5KXNOZonPeRDHzlDj08O67aoashn1c2rgghsfe8JLBQ0KqxsOqqukR68ZnsGXXxxHNIiK8w
                KdFIlElpinKQnnzI46K8jrhqGPuS9RLVAJ9I6q111x2azkM5UCooU1TvZKgWV5fCH22PgDU7oTks8uU1
                8NpkKGlVbPbb1QtEmYBE5fgg4w7kyLwlp8iVSD9DM1Jpa8H03nPqkvsQiJIWoSHEmOVPh2PLKc9tz8Vc
                Cui8dZr7S2dlqxeRzcJfQksBBlVlOAGseScy6im2I4KVay6vyfR7AoIMkjBmsa778HbJNlTqhbhwirxr
                jMo0XZBc1tRMaGFQuRAqIUPQq9HDGiGRvqRhtzovWYi6R5uN4E7eWicuPHm78MFXp9Rwb9nZhXQbuLQN
                XWyg3Jm9H65M4u4zSbOK219fwP8FMwS9p6eZIE3mGTX5411Aj0O7tbUP8zURX3RoWKFby5Kxib796Ui6
                xHvkH3BmgeeDGWpwo5VOli7kBb5QBUWuUXXu39MRFhTndR0KooIgJft2za3Gb6CDTW9J37UR4ZsGKjTK
                8EktP6SduAotuFoOOUOsOzW0J9efvqfIGVVBV57Drf8iM3BmcPiVDKs5qYj6vXB8L4ROYxZlDN8Vi95t
                ihFS8PnKoThX89VKpE892tLfQEwr5l1RH4zqo7vJJXLkFYNrdS4iNs7N5jigYk43Kavyheyaz9rRHSVr
                JBgePbdKZqqy2surSGhRkpkQOcypb6zIBcW3Jshdw6pQTaO7Td43Lbb8T4PoXRrJxDmIuuDOOGf3TgqX
                PPR4pwLy0gNS3UyHs6F0WijjThtH0081D4ay9JHxGZ9caCHJ0peF6daErfWC3Ykr4eoSFA6KHAR8mbHi
                qEKJHLXGFo8H9kWAghP76PzOv2TJnw4caElApI7kcKvUyh3qvSqLnBVOjdKeL1VSIdXiFMnN7t0Kxj60
                NzoVmsCxYQaFGTlhTERU52I0VBjaPFPSOVyP75ctBYZ9H39GDQKJVRf8fhCMcJ4M9WRUaeEEmNpJabOG
                JB6B81voIBeOUcDPevsQoAren65q5DRIiGtdx3qrARUuRb6VSXKZYC9kxB1m7BaDyQZ0KoOhsmMOVw9w
                XsWN5oLRPQa6P3onKfO5yQT90DuaLXXgLsEJXlwCOkh4UhXoEHYHoIiEo7mZoo0kCGvh8Dq22wtFzXun
                Pozg29EqgKlSt4pputp2qGGDswE0O9purWwD6etQXLx0PEDrcDYMDZIXlrbAfOz8FwMF4oFoYXEPyMKy
                kyuQjJEfFAujMwy4vkaZWldz4OWUuIZuVopv88AdVZ60s1xZ2VMSoSXnBfWmJyOKdj0sx8aRjq0rUfsZ
                teo746FVDnhv68nXuLrXqC9q9q2NQig92WbzgBK3vfRWzihDl1uHftzeXXvNXIsMFh7uAguuq6Efs1sq
                GIKtgRynyEymqIvJIseJYV7bHnwXmQkLdNK2WFB6NznVl2lLkxI69ZVxtifu9Hfs8xeKi0OrabVTBBSd
                mw63GNSEczjdgbJNuTDXUffXDtXNywV7LU6DjSmSqnFnEg1eWRSoOneGXbLsq1BxU37uk48ovaQuB98G
                KJDboTxPGOjLDCX6RTF1jQxUvSCsH3qPwFo8r3PK0KVFeYYHeWOZzwPFKbYEG8bGcmEUIrhuTL4XXljQ
                AlMYgHvWbiDfNisfOukNzvlspTK32Vbn85CN9AVg1lKvOqqBhzPOz8umixDbXJc7fLee6toTqojl9nFc
                my0AK0WfHGkeUzEQIHPFNmuszxKU7NBbdDVdWMCn37YEiFnD3xMs1vibyAOewTONc2QpzCgW9elVaMcp
                eplGKAvVnQ3Zt5mjDWBJPH9HVjRdVU1kEexkPrYx9nqnywhxkMi8a0cB2OKjoWh6zdk8VfVTpENnssBI
                j6kQ964zAVLJZ7vpzZefubVsQnHW9rcUfGSAmamDqL1WNGXraXgmN1skW4HQzvKRbMvWWvAIO9uTroqe
                2YfHjzSEYIgLP7OmcPbx4N98bVLfBtpjjIpmXtHTuxMVohy4ilWrGKJR4UNqsih0U828KbCulApG0p4w
                Vs179T0zzhoNdiSnJO3YGD9HxBdPLnLnafTcTimJx8TtWBaSBQrxLlcGssiX6ycZV3hQmEilphO4R2Wz
                jQPYJ8x8DEijxHK3W4jFl7G6tHHRvn3Ng0y60RdWdV6QjDxzpCleFtVLqq51ftr4fxrlqqUg28FFDkvh
                aZKMtyiHg48YKVL7KCPrX7XQm8nATGNwU2ssS8cwGCiA5YMJieeOrxqZS9nSJmubUUZclwmnNQGbgEUW
                ZY8yZlc07fbLY3n14LUFBIeeUuRLakjLOE0bnnNF5Tn8ffAtbOzMyVboU3GSXhNlVE3unFu97WPxiK0X
                Fqn9CY7tywU1jbhsZY8hoLAFbKQwjfpMJlZnetI3jiaKNu8w2xB0hEDI4QJdvtHyMUhGUfQJqYifzENo
                sxjdtcVF9d50VPF35prVqFBokak3iHHHB3mDljUSsB7rncHABFqSlSqHhBDQc1AVKpW6cwShsaiMvNox
                oDcrihiv05BNhxsR9m3nol0bka4Cb4Y4dVMa7eSXxlcZCvelsy7yy7dnOHlVcQSXIOZcgMr2ilKpjWoo
                OIOEiIbsgYoPTwZNeDXlCUOjtWOu0vahNAf3Esb4wYhDzMxM6ouKjq79gUhXNIJsn54yKIdfYCahempd
                8n9k2QiCnYopIdFNuw3UFXh8HsjKPTWJKvoJRMbs0ncstWoj1GzCjN2XW8njosw3UwhkgEPE6ghL4jMY
                TvUQJ9HJKBFwEAtsYrKwDgq0Mw15LUCX3OaXP4bAYRxo0o7fbc66c5OTlZb9uD4lybPousMAMNrSxTuE
                AHo0VcAK5reXVyKrqW8F9hEag2Y96Pur43cn8nkb7MRd1AuBOawzNiUTMm7yHzycBw5GRWr1DjyAcZrY
                oanXHRWPup4zCHNY7X90QzA9EGF0HHc9cqqce4U80BjD6cPGqpyzPjiblp5DzRpHjm4O5H8apzyr52bO
                4uMab8YLwvtbphuCg9G8PGhxaTTBaMZPnOjGRC8WE5aIZA3cA8tgcXh7oreEyoCOUv0gZ3zTmk9OjiYQ
                Hd6fTMpNgHbzQt2Zed7lDgdSLOJDqOOfpSGLtfkuJTy0iYAb8FuyrTRoghB4j9bknumo9oVKKV1qKIyW
                Ie0LMWtYXa4rXyrHc3dENE3oSAAUAveXBgwKxLk9BaCsFyEe6ijRyHBDIexrK7wV1KOEUfejBEPxDxHv
                yNSzhLPDEGkQXW2XNu2smElTEBC1kMdwOk7XuCb78E4f3YVhhAtB1M8RcO5MOHcNi4dJ6D96gRExzC98
                Z1zqFYYf36NZo0Pwr6jr66azrLqVmzXvxLbxwmNXdfER0v8mwkJp3CW7yytmJHiwDvTlIFnjGXTkJQrR
                OITsWkpZj5TvM8Luf4EBAUcQuSX0Stt9wOxq44oo0mJN0kYyOGMPRRyHSv99vkxmVHRhq0rJRcAY7NcN
                aBLIYT0XjNbxdOfgMuM737Bxl7lCGr9G9CpMtNBlVESehmnjDMbhlyzfWfeyGwlshNN4uHu21qgAbE9k"""
        }
    }
])

apply MediaTypeOp @httpRequestTests([
    {
        id: "RpcV2JsonMediaTypeSerialize"
        documentation: """
            A @mediaType string is serialized as an ordinary string. The trait
            documents the contents for tooling and does not change the wire
            form, so a JSON document carried in a string stays escaped rather
            than being inlined into the surrounding body."""
        protocol: rpcv2Json
        method: "POST"
        uri: "/service/RpcV2JsonCorpusTests/operation/MediaTypeOp"
        body: """
            {
                "mediaTypeMember": "{\\\"nested\\\":true}"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json", Accept: "application/json" }
        requireHeaders: ["Content-Length"]
        forbidHeaders: ["X-Amz-Target"]
        params: { mediaTypeMember: "{\"nested\":true}" }
    }
])

apply MediaTypeOp @httpResponseTests([
    {
        id: "RpcV2JsonMediaTypeDeserialize"
        documentation: "A @mediaType string is deserialized as an ordinary string, contents uninterpreted"
        protocol: rpcv2Json
        code: 200
        body: """
            {
                "mediaTypeMember": "{\\\"nested\\\":true}"
            }"""
        bodyMediaType: "application/json"
        headers: { "smithy-protocol": "rpc-v2-json", "Content-Type": "application/json" }
        params: { mediaTypeMember: "{\"nested\":true}" }
    }
])
